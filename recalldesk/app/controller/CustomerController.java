package com.recalldesk.app.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.recalldesk.app.dto.ConversationDto;
import com.recalldesk.app.dto.CustomerDto;
import com.recalldesk.app.dto.MemoryDto;
import com.recalldesk.app.integration.HindsightClient.HindsightRecallResult;
import com.recalldesk.app.memory.HindsightMemoryService;
import com.recalldesk.app.model.Customer;
import com.recalldesk.app.model.MemoryEntry;
import com.recalldesk.app.repository.ConversationRepository;
import com.recalldesk.app.service.ConversationService;
import com.recalldesk.app.service.CustomerService;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final ConversationService conversationService;
    private final HindsightMemoryService memoryService;

    public CustomerController(CustomerService customerService,
                              ConversationService conversationService,
                              HindsightMemoryService memoryService,
                              ConversationRepository conversationRepository) {
        this.customerService = customerService;
        this.conversationService = conversationService;
        this.memoryService = memoryService;
    }

    @GetMapping
    public ResponseEntity<List<CustomerDto>> getAllCustomers(
            @RequestParam(required = false) String search) {
        List<Customer> customers = search != null && !search.isEmpty()
                ? customerService.searchCustomers(search)
                : customerService.getAllCustomers();
        return ResponseEntity.ok(customers.stream()
                .map(CustomerDto::fromEntity)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerDto> getCustomer(@PathVariable String customerId) {
        return customerService.findByCustomerId(customerId)
                .map(c -> ResponseEntity.ok(CustomerDto.fromEntity(c)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CustomerDto> createCustomer(@RequestBody CustomerDto dto) {
        Customer created = customerService.createCustomer(dto);
        return ResponseEntity.ok(CustomerDto.fromEntity(created));
    }

    /**
     * Get all Hindsight memories for a customer.
     * Calls the real Hindsight API to list memories from the cloud.
     */
    @GetMapping("/{customerId}/memories")
    public ResponseEntity<?> getMemories(@PathVariable String customerId) {
        Optional<Customer> customerOpt = customerService.findByCustomerId(customerId);
        if (customerOpt.isEmpty()) return ResponseEntity.notFound().build();

        Customer customer = customerOpt.get();

        // Get from Hindsight (live data)
        HindsightRecallResult hindsightMemories = memoryService.getAllCustomerMemories(customer);

        // Also get local mirror for metadata
        List<MemoryDto> localMemories = memoryService.getLocalMemories(customer)
                .stream().map(MemoryDto::fromEntity).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
            "hindsightMemories", hindsightMemories.getMemories(),
            "localMemories", localMemories,
            "totalFromHindsight", hindsightMemories.getTotalFound(),
            "hindsightAvailable", hindsightMemories.isAvailable(),
            "bankId", customer.getHindsightBankId() != null ? customer.getHindsightBankId() : ""
        ));
    }

    /**
     * Get conversation history for a customer.
     */
    @GetMapping("/{customerId}/conversations")
    public ResponseEntity<?> getConversations(@PathVariable String customerId) {
        return customerService.findByCustomerId(customerId)
                .map(c -> {
                    List<ConversationDto> convs = conversationService.getCustomerConversations(c)
                            .stream().map(ConversationDto::fromEntity).collect(Collectors.toList());
                    return ResponseEntity.ok(convs);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Get memory timeline for a customer — ordered events for display.
     */
    @GetMapping("/{customerId}/timeline")
    public ResponseEntity<?> getTimeline(@PathVariable String customerId) {
        Optional<Customer> customerOpt = customerService.findByCustomerId(customerId);
        if (customerOpt.isEmpty()) return ResponseEntity.notFound().build();

        Customer customer = customerOpt.get();
        List<MemoryDto> memories = memoryService.getLocalMemories(customer)
                .stream().map(MemoryDto::fromEntity).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
            "customerId", customerId,
            "customerName", customer.getName(),
            "timeline", memories,
            "totalEvents", memories.size()
        ));
    }

    /**
     * Memory Impact: compare generic vs memory-aware responses.
     */
    @GetMapping("/{customerId}/memory-impact")
    public ResponseEntity<?> getMemoryImpact(@PathVariable String customerId,
                                              @RequestParam String query) {
        Optional<Customer> customerOpt = customerService.findByCustomerId(customerId);
        if (customerOpt.isEmpty()) return ResponseEntity.notFound().build();

        Customer customer = customerOpt.get();
        HindsightRecallResult memories = memoryService.retrieveRelevantMemories(customer, query);

        return ResponseEntity.ok(Map.of(
            "customerId", customerId,
            "customerName", customer.getName(),
            "query", query,
            "memories", memories.getMemories(),
            "memoriesCount", memories.getMemories().size(),
            "hindsightAvailable", memories.isAvailable()
        ));
    }

    /**
     * Search memories for a customer using natural language.
     */
    @PostMapping("/{customerId}/memories/search")
    public ResponseEntity<?> searchMemories(@PathVariable String customerId,
                                             @RequestBody Map<String, String> body) {
        String query = body.get("query");
        if (query == null || query.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Query required"));
        }

        Optional<Customer> customerOpt = customerService.findByCustomerId(customerId);
        if (customerOpt.isEmpty()) return ResponseEntity.notFound().build();

        Customer customer = customerOpt.get();
        HindsightRecallResult result = memoryService.retrieveRelevantMemories(customer, query);

        return ResponseEntity.ok(Map.of(
            "results", result.getMemories(),
            "count", result.getMemories().size(),
            "hindsightAvailable", result.isAvailable(),
            "bankId", customer.getHindsightBankId() != null ? customer.getHindsightBankId() : ""
        ));
    }

    /**
     * Manually add a memory for a customer.
     */
    @PostMapping("/{customerId}/memories")
    public ResponseEntity<?> addMemory(@PathVariable String customerId,
                                       @RequestBody MemoryDto dto) {
        Optional<Customer> customerOpt = customerService.findByCustomerId(customerId);
        if (customerOpt.isEmpty()) return ResponseEntity.notFound().build();

        Customer customer = customerOpt.get();
        MemoryEntry.MemoryCategory cat = MemoryEntry.MemoryCategory.CUSTOM;
        MemoryEntry.Importance imp = MemoryEntry.Importance.MEDIUM;
        try { cat = MemoryEntry.MemoryCategory.valueOf(dto.getCategory()); } catch (Exception ignored) {}
        try { imp = MemoryEntry.Importance.valueOf(dto.getImportance()); } catch (Exception ignored) {}

        MemoryEntry saved = memoryService.storeMemory(customer, dto.getContent(), cat, imp, null);
        return ResponseEntity.ok(MemoryDto.fromEntity(saved));
    }

    /**
     * Reflect on a customer — AI-powered synthesis from Hindsight.
     */
    @PostMapping("/{customerId}/reflect")
    public ResponseEntity<?> reflect(@PathVariable String customerId,
                                     @RequestBody Map<String, String> body) {
        String question = body.getOrDefault("question",
                "What do we know about this customer and what should we prioritize?");

        Optional<Customer> customerOpt = customerService.findByCustomerId(customerId);
        if (customerOpt.isEmpty()) return ResponseEntity.notFound().build();

        var result = memoryService.reflectOnCustomer(customerOpt.get(), question);
        return ResponseEntity.ok(Map.of(
            "answer", result.getText() != null ? result.getText() : "",
            "basedOn", result.getBasedOn(),
            "hindsightAvailable", result.isAvailable()
        ));
    }
}
