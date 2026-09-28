package com.recalldesk.app.memory;

import com.recalldesk.app.integration.HindsightClient;
import com.recalldesk.app.integration.HindsightClient.HindsightRecallResult;
import com.recalldesk.app.integration.HindsightClient.HindsightReflectResult;
import com.recalldesk.app.model.Customer;
import com.recalldesk.app.model.MemoryEntry;
import com.recalldesk.app.repository.CustomerRepository;
import com.recalldesk.app.repository.MemoryEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * HindsightMemoryService — the central memory abstraction for RecallDesk AI.
 */
@Service
public class HindsightMemoryService {

    private static final Logger log = LoggerFactory.getLogger(HindsightMemoryService.class);

    private final HindsightClient hindsightClient;
    private final MemoryEntryRepository memoryEntryRepository;
    private final CustomerRepository customerRepository;

    public HindsightMemoryService(HindsightClient hindsightClient,
                                   MemoryEntryRepository memoryEntryRepository,
                                   CustomerRepository customerRepository) {
        this.hindsightClient = hindsightClient;
        this.memoryEntryRepository = memoryEntryRepository;
        this.customerRepository = customerRepository;
    }

    // -----------------------------------------------------------------------
    // Bank Initialization
    // -----------------------------------------------------------------------

    /**
     * Ensure a Hindsight memory bank exists for this customer.
     * Called when a customer is first created or when a conversation starts.
     */
    public String initCustomerBank(Customer customer) {
        String bankId = customer.getHindsightBankId();
        if (bankId == null || bankId.isEmpty()) {
            // Create a stable, unique bank ID
            bankId = "customer-" + customer.getCustomerId().toLowerCase().replace(" ", "-");
            customer.setHindsightBankId(bankId);
            customerRepository.save(customer);
        }

        String background = String.format(
            "Customer support memory bank for %s (ID: %s). " +
            "Customer is on the %s plan at %s. " +
            "This bank stores facts, preferences, past issues, and resolutions " +
            "to help support agents provide personalized, context-aware assistance.",
            customer.getName(),
            customer.getCustomerId(),
            customer.getPlan().name(),
            customer.getCompany() != null ? customer.getCompany() : "their company"
        );

        hindsightClient.createBank(bankId, "Support Memory — " + customer.getName(), background);
        log.info("Hindsight bank ready for customer {}: {}", customer.getCustomerId(), bankId);
        return bankId;
    }

    // -----------------------------------------------------------------------
    // Store Memories
    // -----------------------------------------------------------------------

    /**
     * Store a memory fact about a customer.
     *
     * @param customer   The customer this memory belongs to
     * @param content    The fact/information to remember
     * @param category   Type of memory (PREFERENCE, PAST_PROBLEM, etc.)
     * @param importance How important this memory is
     * @param conversationId Source conversation ID (optional)
     */
    @Transactional
    public MemoryEntry storeMemory(Customer customer,
                                    String content,
                                    MemoryEntry.MemoryCategory category,
                                    MemoryEntry.Importance importance,
                                    Long conversationId) {
        String bankId = initCustomerBank(customer);
        String docId = "mem_" + category.name().toLowerCase() + "_" + UUID.randomUUID().toString().substring(0, 8);

        // Store in Hindsight — the real persistent memory
        Map<String, String> metadata = Map.of(
            "category", category.name(),
            "importance", importance.name(),
            "customer_id", customer.getCustomerId()
        );
        String context = "customer support — " + category.name().toLowerCase().replace("_", " ");
        hindsightClient.retain(bankId, content, context, docId, metadata);

        // Mirror locally for fast UI rendering
        MemoryEntry entry = MemoryEntry.builder()
                .customer(customer)
                .content(content)
                .category(category)
                .importance(importance)
                .conversationId(conversationId)
                .hindsightDocumentId(docId)
                .createdAt(LocalDateTime.now())
                .build();

        MemoryEntry saved = memoryEntryRepository.save(entry);

        // Update customer memory count
        customer.setMemoriesCount(customer.getMemoriesCount() + 1);
        customerRepository.save(customer);

        log.info("Memory stored for {}: [{}] {}", customer.getCustomerId(), category, content.substring(0, Math.min(60, content.length())));
        return saved;
    }

    /**
     * Store a full conversation transcript into Hindsight for memory extraction.
     * Hindsight will automatically extract facts, preferences, and issues from it.
     */
    public void storeConversationTranscript(Customer customer,
                                             String transcript,
                                             String conversationId,
                                             String subject) {
        String bankId = initCustomerBank(customer);
        boolean success = hindsightClient.retainConversation(bankId, transcript, conversationId, subject);
        log.info("Conversation transcript retained for {}: conv={}, success={}",
                customer.getCustomerId(), conversationId, success);
    }

    // -----------------------------------------------------------------------
    // Retrieve Memories
    // -----------------------------------------------------------------------

    /**
     * Retrieve memories from Hindsight relevant to the current customer message.
     * This is the CORE function that makes the AI context-aware.
     *
     * @param customer       The customer sending the message
     * @param currentMessage The customer's current message
     * @return Recalled memories from Hindsight
     */
    public HindsightRecallResult retrieveRelevantMemories(Customer customer, String currentMessage) {
        String bankId = initCustomerBank(customer);

        // Build a rich query that captures what we need to know
        String query = String.format(
            "What do we know about this customer relevant to: %s. " +
            "Include: previous issues, resolutions, preferences, commitments, and sentiment.",
            currentMessage
        );

        HindsightRecallResult result = hindsightClient.recall(bankId, query, 2048);

        if (result.isAvailable() && result.hasMemories()) {
            log.info("Hindsight recalled {} memories for customer {} on query: {}",
                    result.getMemories().size(),
                    customer.getCustomerId(),
                    currentMessage.substring(0, Math.min(50, currentMessage.length())));
        } else if (!result.isAvailable()) {
            log.warn("Hindsight unavailable for customer {}: {}", customer.getCustomerId(), result.getErrorMessage());
        } else {
            log.info("No memories found for customer {} — this is their first interaction", customer.getCustomerId());
        }

        return result;
    }

    /**
     * Get all memories for a customer (for profile/dashboard display).
     */
    public HindsightRecallResult getAllCustomerMemories(Customer customer) {
        String bankId = initCustomerBank(customer);
        return hindsightClient.listMemories(bankId, 50);
    }

    /**
     * Get local memory entries for a customer (fast, no API call).
     */
    public List<MemoryEntry> getLocalMemories(Customer customer) {
        return memoryEntryRepository.findByCustomerOrderByCreatedAtDesc(customer);
    }

    // -----------------------------------------------------------------------
    // Build Context for LLM
    // -----------------------------------------------------------------------

    /**
     * Format recalled Hindsight memories into a structured context block
     * that can be injected into the LLM prompt.
     *
     * @param recallResult The memories retrieved from Hindsight
     * @param customerName The customer's name for personalization
     * @return Formatted context string for the LLM prompt
     */
    public String buildMemoryContext(HindsightRecallResult recallResult, String customerName) {
        if (recallResult == null || !recallResult.hasMemories()) {
            return "No prior memory found for this customer. This appears to be their first interaction.";
        }

        StringBuilder context = new StringBuilder();
        context.append("=== HINDSIGHT CUSTOMER MEMORY (").append(recallResult.getMemories().size()).append(" memories retrieved) ===\n\n");
        context.append("What we know about ").append(customerName).append(":\n");

        for (HindsightClient.MemoryItem memory : recallResult.getMemories()) {
            context.append("• ").append(memory.getText()).append("\n");
        }

        context.append("\n[Memory retrieved from Hindsight persistent memory system]");
        context.append("\n[Use this context to personalize your response — do not repeat these facts verbatim]");

        return context.toString();
    }

    /**
     * Get an AI-powered reflection on what we know about a customer.
     */
    public HindsightReflectResult reflectOnCustomer(Customer customer, String question) {
        String bankId = initCustomerBank(customer);
        String context = "Customer support context for " + customer.getName();
        return hindsightClient.reflect(bankId, question, context);
    }

    // -----------------------------------------------------------------------
    // Update Memory
    // -----------------------------------------------------------------------

    /**
     * Mark a memory/issue as resolved.
     */
    @Transactional
    public void resolveIssue(Long memoryEntryId) {
        memoryEntryRepository.findById(memoryEntryId).ifPresent(entry -> {
            entry.setResolved(true);
            entry.setUpdatedAt(LocalDateTime.now());
            memoryEntryRepository.save(entry);

            // Update in Hindsight with a resolution note
            String bankId = entry.getCustomer().getHindsightBankId();
            if (bankId != null) {
                String resolvedContent = entry.getContent() + " [RESOLVED]";
                hindsightClient.retain(bankId, resolvedContent,
                        "issue resolution", entry.getHindsightDocumentId(), null);
            }
        });
    }

    /**
     * Increment retrieval count for a memory (tracking how useful each memory is).
     */
    @Transactional
    public void incrementRetrievalCount(Customer customer) {
        List<MemoryEntry> entries = memoryEntryRepository.findByCustomerOrderByCreatedAtDesc(customer);
        // Track on most recent entries
        entries.stream().limit(3).forEach(entry -> {
            entry.setRetrievalCount(entry.getRetrievalCount() + 1);
            memoryEntryRepository.save(entry);
        });
    }

    // -----------------------------------------------------------------------
    // Check availability
    // -----------------------------------------------------------------------

    public boolean isHindsightAvailable() {
        // Ping by doing a fast recall on a dummy query
        try {
            hindsightClient.recall("health-check", "ping", 10);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
