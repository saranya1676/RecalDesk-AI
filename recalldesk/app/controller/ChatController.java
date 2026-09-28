package com.recalldesk.app.controller;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.recalldesk.app.dto.ChatRequestDto;
import com.recalldesk.app.dto.ChatResponseDto;
import com.recalldesk.app.model.Conversation;
import com.recalldesk.app.model.Customer;
import com.recalldesk.app.service.AiAgentService;
import com.recalldesk.app.service.ConversationService;
import com.recalldesk.app.service.CustomerService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final AiAgentService aiAgentService;
    private final CustomerService customerService;
    private final ConversationService conversationService;

    public ChatController(AiAgentService aiAgentService,                          CustomerService customerService,
                          ConversationService conversationService) {
        this.aiAgentService = aiAgentService;
        this.customerService = customerService;
        this.conversationService = conversationService;
    }

    /**
     * Main chat endpoint — receives a message and returns an AI response
     * with full memory context from Hindsight.
     */
    @PostMapping("/message")
    public ResponseEntity<?> sendMessage(@Valid @RequestBody ChatRequestDto request) {
        // 1. Identify customer
        Optional<Customer> customerOpt = resolveCustomer(request.getCustomerId());
        if (customerOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Customer not found: " + request.getCustomerId(),
                "success", false
            ));
        }
        Customer customer = customerOpt.get();

        // 2. Resolve or create conversation
        Conversation conversation;
        if (request.getConversationId() != null && !request.getConversationId().isEmpty()) {
            Optional<Conversation> convOpt = conversationService.findByConversationId(request.getConversationId());
            if (convOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Conversation not found: " + request.getConversationId(),
                    "success", false
                ));
            }
            conversation = convOpt.get();
        } else {
            // Create a new conversation
            Conversation.Category cat = Conversation.Category.GENERAL;
            if (request.getCategory() != null) {
                try { cat = Conversation.Category.valueOf(request.getCategory()); } catch (Exception ignored) {}
            }
            conversation = conversationService.createConversation(customer, request.getSubject(), cat);
            customerService.updateLastInteraction(customer);
        }

        // 3. Process with AI pipeline (includes Hindsight memory retrieval)
        ChatResponseDto response = aiAgentService.processMessage(customer, conversation, request.getMessage());
        response.setConversationId(conversation.getConversationId());
        response.setCustomerName(customer.getName());
        response.setCustomerPlan(customer.getPlan().name());

        return ResponseEntity.ok(response);
    }

    /**
     * Get a generic (no-memory) response for Memory Impact comparison.
     */
    @PostMapping("/generic-response")
    public ResponseEntity<?> getGenericResponse(@RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "");
        if (message.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message required"));
        }
        String response = aiAgentService.generateGenericResponse(message, Conversation.Category.GENERAL);
        return ResponseEntity.ok(Map.of("response", response, "memoryUsed", false));
    }

    /**
     * Get a personalized (with-memory) response for Memory Impact comparison.
     */
    @PostMapping("/personalized-response")
    public ResponseEntity<?> getPersonalizedResponse(@RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "");
        String customerId = body.getOrDefault("customerId", "");

        if (message.isEmpty() || customerId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "message and customerId required"));
        }

        Optional<Customer> customerOpt = resolveCustomer(customerId);
        if (customerOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Customer not found"));
        }

        String response = aiAgentService.generatePersonalizedResponse(customerOpt.get(), message);
        return ResponseEntity.ok(Map.of("response", response, "memoryUsed", true));
    }

    /**
     * AI action: Summarize customer history.
     */
    @GetMapping("/action/summarize/{customerId}")
    public ResponseEntity<?> summarizeHistory(@PathVariable String customerId) {
        return resolveCustomer(customerId)
                .map(c -> ResponseEntity.ok(Map.of(
                    "result", aiAgentService.summarizeCustomerHistory(c),
                    "action", "summarize"
                )))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * AI action: Find unresolved issues.
     */
    @GetMapping("/action/unresolved/{customerId}")
    public ResponseEntity<?> findUnresolved(@PathVariable String customerId) {
        return resolveCustomer(customerId)
                .map(c -> ResponseEntity.ok(Map.of(
                    "result", aiAgentService.findUnresolvedIssues(c),
                    "action", "unresolved_issues"
                )))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * AI action: Identify customer sentiment.
     */
    @GetMapping("/action/sentiment/{customerId}")
    public ResponseEntity<?> identifySentiment(@PathVariable String customerId) {
        return resolveCustomer(customerId)
                .map(c -> ResponseEntity.ok(Map.of(
                    "result", aiAgentService.identifySentiment(c),
                    "action", "sentiment"
                )))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * AI action: Generate follow-up message.
     */
    @PostMapping("/action/followup/{customerId}")
    public ResponseEntity<?> generateFollowUp(@PathVariable String customerId,
                                              @RequestBody Map<String, String> body) {
        return resolveCustomer(customerId)
                .map(c -> ResponseEntity.ok(Map.of(
                    "result", aiAgentService.generateFollowUp(c, body.get("context")),
                    "action", "followup"
                )))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Resolve customer by ID or email
    private Optional<Customer> resolveCustomer(String identifier) {
        if (identifier == null || identifier.isEmpty()) return Optional.empty();
        if (identifier.contains("@")) {
            return customerService.findByEmail(identifier);
        }
        // Try customerId first, then numeric ID
        Optional<Customer> byCustomerId = customerService.findByCustomerId(identifier);
        if (byCustomerId.isPresent()) return byCustomerId;
        try {
            return customerService.findById(Long.parseLong(identifier));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
