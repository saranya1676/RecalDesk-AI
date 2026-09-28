package com.recalldesk.app.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recalldesk.app.dto.ChatResponseDto;
import com.recalldesk.app.integration.GroqClient;
import com.recalldesk.app.integration.GroqClient.GroqMessage;
import com.recalldesk.app.integration.GroqClient.GroqResponse;
import com.recalldesk.app.integration.HindsightClient.HindsightRecallResult;
import com.recalldesk.app.integration.HindsightClient.MemoryItem;
import com.recalldesk.app.memory.HindsightMemoryService;
import com.recalldesk.app.model.ChatMessage;
import com.recalldesk.app.model.Conversation;
import com.recalldesk.app.model.Customer;
import com.recalldesk.app.model.MemoryEntry;
import com.recalldesk.app.repository.ChatMessageRepository;
import com.recalldesk.app.repository.ConversationRepository;

/**
 * AiAgentService — orchestrates the full memory-aware AI pipeline.
 */
@Service
public class AiAgentService {

    private static final Logger log = LoggerFactory.getLogger(AiAgentService.class);

    private final GroqClient groqClient;
    private final HindsightMemoryService memoryService;
    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ObjectMapper objectMapper;

    public AiAgentService(GroqClient groqClient,
                          HindsightMemoryService memoryService,
                          ConversationRepository conversationRepository,
                          ChatMessageRepository chatMessageRepository,
                          ObjectMapper objectMapper) {
        this.groqClient = groqClient;
        this.memoryService = memoryService;
        this.conversationRepository = conversationRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.objectMapper = objectMapper;
    }

    // -----------------------------------------------------------------------
    // Process Customer Message
    // -----------------------------------------------------------------------

    /**
     * Process a customer message with full memory context.
     * This is the main entry point for the AI pipeline.
     */
    @Transactional
    public ChatResponseDto processMessage(Customer customer,
                                           Conversation conversation,
                                           String userMessage) {
        log.info("Processing message for customer {}: '{}'",
                customer.getCustomerId(),
                userMessage.substring(0, Math.min(60, userMessage.length())));

        // STEP 1: Save the user message
        ChatMessage userMsg = ChatMessage.builder()
                .conversation(conversation)
                .role(ChatMessage.Role.USER)
                .content(userMessage)
                .createdAt(LocalDateTime.now())
                .build();
        chatMessageRepository.save(userMsg);

        // STEP 2: Retrieve relevant memories from Hindsight
        HindsightRecallResult memories = memoryService.retrieveRelevantMemories(customer, userMessage);
        boolean hindsightAvailable = memories.isAvailable();
        boolean hasMemories = memories.hasMemories();

        // STEP 3: Get recent conversation history
        List<ChatMessage> recentHistory = chatMessageRepository
                .findTop10ByConversationOrderByCreatedAtDesc(conversation)
                .stream()
                .filter(m -> !m.getId().equals(userMsg.getId()))
                .collect(Collectors.toList());
        // Reverse to chronological order
        java.util.Collections.reverse(recentHistory);

        // STEP 4: Build the LLM prompt
        String systemPrompt = buildSystemPrompt(customer, memories, hindsightAvailable);
        List<GroqMessage> messages = buildMessageList(systemPrompt, recentHistory, userMessage);

        // STEP 5: Call Groq LLM
        GroqResponse groqResponse = groqClient.chat(messages);

        // STEP 6: Extract memory impact explanation
        String memoryImpactExplanation = buildMemoryImpactExplanation(memories, userMessage, customer);

        // STEP 7: Save the AI response message
        String memoriesJson = serializeMemories(memories);
        ChatMessage aiMsg = ChatMessage.builder()
                .conversation(conversation)
                .role(ChatMessage.Role.AGENT)
                .content(groqResponse.getContent())
                .createdAt(LocalDateTime.now())
                .memoryUsed(hasMemories)
                .memoriesUsedJson(memoriesJson)
                .memoryImpactExplanation(memoryImpactExplanation)
                .memoriesCount(hasMemories ? memories.getMemories().size() : 0)
                .build();
        chatMessageRepository.save(aiMsg);

        // STEP 8: Update conversation metadata
        conversation.setLastMessageAt(LocalDateTime.now());
        conversation.setHindsightUsed(conversation.getHindsightUsed() || hasMemories);
        conversation.setMemoriesRetrieved(
            conversation.getMemoriesRetrieved() + (hasMemories ? memories.getMemories().size() : 0));
        conversationRepository.save(conversation);

        // STEP 9: Store the conversation update in Hindsight (async-style — store after response)
        storeConversationMemory(customer, conversation, userMessage, groqResponse.getContent());

        // STEP 10: Build and return response DTO
        return buildResponseDto(groqResponse, memories, memoryImpactExplanation,
                hindsightAvailable, aiMsg.getId());
    }

    // -----------------------------------------------------------------------
    // Generate "Without Memory" response for Memory Impact comparison
    // -----------------------------------------------------------------------

    /**
     * Generate a GENERIC response without any memory context.
     * Used for the Memory Impact comparison feature.
     */
    public String generateGenericResponse(String userMessage, Conversation.Category category) {
        String systemPrompt = """
                You are a generic customer support agent with NO prior knowledge of this customer.
                You have no memory of previous interactions.
                Provide a helpful but completely generic response.
                Do NOT personalize. Do NOT reference any prior history.
                Be professional but generic — like responding to a completely new customer.
                Keep response to 2-3 sentences maximum.
                """;

        GroqResponse response = groqClient.complete(systemPrompt, userMessage);
        return response.getContent();
    }

    /**
     * Generate a PERSONALIZED response with full memory context.
     * Used for Memory Impact comparison.
     */
    public String generatePersonalizedResponse(Customer customer, String userMessage) {
        HindsightRecallResult memories = memoryService.retrieveRelevantMemories(customer, userMessage);
        String systemPrompt = buildSystemPrompt(customer, memories, memories.isAvailable());
        GroqResponse response = groqClient.complete(systemPrompt,
                "Customer message: " + userMessage);
        return response.getContent();
    }

    // -----------------------------------------------------------------------
    // AI-Powered Actions
    // -----------------------------------------------------------------------

    /**
     * Summarize a customer's complete history using Hindsight reflect.
     */
    public String summarizeCustomerHistory(Customer customer) {
        var reflectResult = memoryService.reflectOnCustomer(customer,
            "Provide a concise summary of this customer's history: " +
            "their plan, key issues, resolutions, preferences, and current status.");

        if (reflectResult.isAvailable() && reflectResult.getText() != null
                && !reflectResult.getText().isEmpty()) {
            return reflectResult.getText();
        }

        // Fallback: ask Groq based on local memories
        List<MemoryEntry> localMemories = memoryService.getLocalMemories(customer);
        if (localMemories.isEmpty()) {
            return "No history available for this customer yet.";
        }

        String memorySummary = localMemories.stream()
                .map(m -> "- [" + m.getCategory().name() + "] " + m.getContent())
                .limit(15)
                .collect(Collectors.joining("\n"));

        String prompt = "Summarize this customer's support history in 3-4 sentences:\n" + memorySummary;
        GroqResponse response = groqClient.complete(
            "You are a customer success analyst. Be concise and factual.", prompt);
        return response.getContent();
    }

    /**
     * Find all unresolved issues for a customer.
     */
    public String findUnresolvedIssues(Customer customer) {
        var reflectResult = memoryService.reflectOnCustomer(customer,
            "What unresolved issues or pending items does this customer have? " +
            "List them clearly with any context.");

        if (reflectResult.isAvailable() && reflectResult.getText() != null
                && !reflectResult.getText().isEmpty()) {
            return reflectResult.getText();
        }

        List<MemoryEntry> openIssues = memoryService.getLocalMemories(customer).stream()
                .filter(m -> m.getCategory() == MemoryEntry.MemoryCategory.UNRESOLVED_ISSUE
                        && !m.getResolved())
                .collect(Collectors.toList());

        if (openIssues.isEmpty()) {
            return "No unresolved issues found for this customer.";
        }

        return openIssues.stream()
                .map(m -> "• " + m.getContent())
                .collect(Collectors.joining("\n"));
    }

    /**
     * Identify customer sentiment using Hindsight memories.
     */
    public String identifySentiment(Customer customer) {
        var reflectResult = memoryService.reflectOnCustomer(customer,
            "What is this customer's current sentiment and emotional state? " +
            "Based on their history, are they satisfied, frustrated, or neutral?");

        if (reflectResult.isAvailable() && reflectResult.getText() != null
                && !reflectResult.getText().isEmpty()) {
            return reflectResult.getText();
        }

        return "Current sentiment: " + customer.getCurrentSentiment().name().replace("_", " ");
    }

    /**
     * Generate a follow-up message for a customer.
     */
    public String generateFollowUp(Customer customer, String context) {
        HindsightRecallResult memories = memoryService.retrieveRelevantMemories(
                customer, "follow up on previous issues and commitments");

        String memContext = memoryService.buildMemoryContext(memories, customer.getName());
        String prompt = String.format(
            "%s\n\nContext: %s\n\nWrite a professional follow-up email/message for %s " +
            "addressing their previous issues and any pending commitments.",
            memContext, context != null ? context : "General follow-up",
            customer.getName()
        );

        GroqResponse response = groqClient.complete(
            "You are a customer success manager writing personalized follow-up messages.", prompt);
        return response.getContent();
    }

    // -----------------------------------------------------------------------
    // Prompt Construction
    // -----------------------------------------------------------------------

    private String buildSystemPrompt(Customer customer, HindsightRecallResult memories,
                                      boolean hindsightAvailable) {
        StringBuilder systemPrompt = new StringBuilder();

        // Core identity
        systemPrompt.append("""
            You are RecallDesk AI, an intelligent customer support agent that remembers customers across conversations.
            You provide personalized, empathetic support using persistent memory of each customer's history.
            
            IMPORTANT RULES:
            - Use the customer memory context below to personalize your response
            - Reference relevant past issues naturally (not by saying "as I remember from before")
            - Acknowledge patterns you've noticed (e.g., if they've had billing issues before)
            - Be concise and professional
            - If a past issue was resolved, acknowledge that and check if it's still resolved
            - If there are open/unresolved issues, address them proactively
            - Adapt your communication style to the customer's stated preferences
            
            """);

        // Customer context
        systemPrompt.append("CUSTOMER CONTEXT:\n");
        systemPrompt.append("Name: ").append(customer.getName()).append("\n");
        systemPrompt.append("Plan: ").append(customer.getPlan().name()).append("\n");
        if (customer.getCompany() != null) {
            systemPrompt.append("Company: ").append(customer.getCompany()).append("\n");
        }
        systemPrompt.append("Current sentiment: ").append(customer.getCurrentSentiment().name().replace("_", " ")).append("\n\n");

        // Hindsight memory context
        if (hindsightAvailable && memories.hasMemories()) {
            systemPrompt.append(memoryService.buildMemoryContext(memories, customer.getName()));
        } else if (!hindsightAvailable) {
            systemPrompt.append("[Note: Hindsight memory system temporarily unavailable. Responding based on current context only.]\n");
        } else {
            systemPrompt.append("[No prior memory found. This appears to be the customer's first interaction.]\n");
        }

        return systemPrompt.toString();
    }

    private List<GroqMessage> buildMessageList(String systemPrompt,
                                                List<ChatMessage> history,
                                                String currentMessage) {
        List<GroqMessage> messages = new ArrayList<>();
        messages.add(new GroqMessage("system", systemPrompt));

        // Add recent conversation history (max 8 turns to stay within token limits)
        int startIdx = Math.max(0, history.size() - 8);
        for (int i = startIdx; i < history.size(); i++) {
            ChatMessage msg = history.get(i);
            String role = msg.getRole() == ChatMessage.Role.USER ? "user" : "assistant";
            messages.add(new GroqMessage(role, msg.getContent()));
        }

        // Add current user message
        messages.add(new GroqMessage("user", currentMessage));
        return messages;
    }

    // -----------------------------------------------------------------------
    // Memory Extraction & Storage
    // -----------------------------------------------------------------------

    private void storeConversationMemory(Customer customer, Conversation conversation,
                                          String userMessage, String aiResponse) {
        try {
            // Store the conversation exchange in Hindsight
            String transcript = String.format(
                "Customer (%s): %s\nSupport Agent: %s",
                LocalDateTime.now(), userMessage, aiResponse
            );
            memoryService.storeConversationTranscript(
                customer, transcript,
                conversation.getConversationId(),
                conversation.getSubject()
            );

            // Extract and store specific memory types using Groq
            extractAndStoreMemories(customer, userMessage, aiResponse, conversation.getId());

        } catch (Exception e) {
            log.warn("Failed to store conversation memory for {}: {}",
                    customer.getCustomerId(), e.getMessage());
        }
    }

    private void extractAndStoreMemories(Customer customer, String userMessage,
                                          String aiResponse, Long conversationId) {
        // Ask Groq to extract memory-worthy facts from this exchange
        String extractionPrompt = String.format("""
            Analyze this customer support exchange and extract memory-worthy facts.
            
            Customer: %s
            AI Response: %s
            
            Extract ONLY genuinely new, important facts (not obvious things).
            For each fact, specify:
            - CATEGORY: one of [PREFERENCE, PAST_PROBLEM, RESOLUTION, UNRESOLVED_ISSUE, BEHAVIORAL, COMMITMENT, PRODUCT_USAGE, CUSTOMER_FACT]
            - IMPORTANCE: one of [LOW, MEDIUM, HIGH, CRITICAL]
            - FACT: the specific fact to remember
            
            Format EXACTLY as:
            MEMORY: [CATEGORY] [IMPORTANCE] The fact to remember.
            
            Extract 1-3 facts maximum. Only extract genuinely useful information.
            If nothing important, respond with: MEMORY: NONE
            """, userMessage, aiResponse);

        GroqResponse extraction = groqClient.complete(
            "You are a memory extraction system. Extract only important, factual information.", 
            extractionPrompt);

        if (extraction.isSuccess() && extraction.getContent() != null) {
            parseAndStoreMemoryExtractions(customer, extraction.getContent(), conversationId);
        }
    }

    private void parseAndStoreMemoryExtractions(Customer customer, String extractionText, Long conversationId) {
        if (extractionText.contains("NONE")) return;

        String[] lines = extractionText.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (!line.startsWith("MEMORY:")) continue;
            
            try {
                String content = line.substring(7).trim();
                if (content.equals("NONE")) continue;

                // Parse [CATEGORY] [IMPORTANCE] fact
                MemoryEntry.MemoryCategory category = MemoryEntry.MemoryCategory.CUSTOMER_FACT;
                MemoryEntry.Importance importance = MemoryEntry.Importance.MEDIUM;

                if (content.startsWith("[")) {
                    int catEnd = content.indexOf("]");
                    if (catEnd > 0) {
                        String catStr = content.substring(1, catEnd).trim();
                        try { category = MemoryEntry.MemoryCategory.valueOf(catStr); } catch (Exception ignored) {}
                        content = content.substring(catEnd + 1).trim();
                    }
                }
                if (content.startsWith("[")) {
                    int impEnd = content.indexOf("]");
                    if (impEnd > 0) {
                        String impStr = content.substring(1, impEnd).trim();
                        try { importance = MemoryEntry.Importance.valueOf(impStr); } catch (Exception ignored) {}
                        content = content.substring(impEnd + 1).trim();
                    }
                }

                if (!content.isEmpty() && content.length() > 10) {
                    memoryService.storeMemory(customer, content, category, importance, conversationId);
                }
            } catch (Exception e) {
                log.debug("Could not parse memory line: {}", line);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private String buildMemoryImpactExplanation(HindsightRecallResult memories,
                                                  String userMessage,
                                                  Customer customer) {
        if (!memories.hasMemories()) {
            return "No prior memory found. This is a fresh start — any information shared now will be remembered for future conversations.";
        }

        // Ask Groq to explain why the memories matter for THIS query
        String prompt = String.format("""
            Customer query: "%s"
            
            Memories retrieved from Hindsight:
            %s
            
            In 2-3 sentences, explain specifically why these memories are relevant to the customer's query
            and how they changed the response. Be specific about what would have been missed without memory.
            """,
            userMessage,
            memories.getMemories().stream()
                .map(m -> "- " + m.getText())
                .limit(5)
                .collect(Collectors.joining("\n"))
        );

        GroqResponse explanation = groqClient.complete(
            "You explain why AI memory matters for customer support. Be concise and specific.", prompt);

        if (explanation.isSuccess() && explanation.getContent() != null) {
            return explanation.getContent();
        }

        // Fallback
        return String.format(
            "Because this customer's history was retrieved from Hindsight, " +
            "the response was personalized using %d relevant memories, " +
            "including context about their %s.",
            memories.getMemories().size(),
            customer.getCurrentSentiment().name().toLowerCase().replace("_", " ") + " sentiment"
        );
    }

    private String serializeMemories(HindsightRecallResult memories) {
        if (!memories.hasMemories()) return "[]";
        try {
            List<String> texts = memories.getMemories().stream()
                    .map(MemoryItem::getText)
                    .collect(Collectors.toList());
            return objectMapper.writeValueAsString(texts);
        } catch (Exception e) {
            return "[]";
        }
    }

    private ChatResponseDto buildResponseDto(GroqResponse groqResponse,
                                              HindsightRecallResult memories,
                                              String memoryImpactExplanation,
                                              boolean hindsightAvailable,
                                              Long messageId) {
        List<String> memoryTexts = memories.hasMemories()
                ? memories.getMemories().stream().map(MemoryItem::getText).collect(Collectors.toList())
                : List.of();

        return ChatResponseDto.builder()
                .messageId(messageId)
                .content(groqResponse.getContent())
                .memoriesUsed(memoryTexts)
                .memoriesCount(memoryTexts.size())
                .memoryImpactExplanation(memoryImpactExplanation)
                .hindsightAvailable(hindsightAvailable)
                .memoryUsed(memories.hasMemories())
                .success(groqResponse.isSuccess())
                .errorMessage(groqResponse.getErrorMessage())
                .build();
    }
}
