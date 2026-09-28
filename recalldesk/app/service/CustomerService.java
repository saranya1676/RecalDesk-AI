package com.recalldesk.app.service;

import com.recalldesk.app.dto.CustomerDto;
import com.recalldesk.app.memory.HindsightMemoryService;
import com.recalldesk.app.model.Customer;
import com.recalldesk.app.model.MemoryEntry;
import com.recalldesk.app.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;
    private final HindsightMemoryService memoryService;

    public CustomerService(CustomerRepository customerRepository, HindsightMemoryService memoryService) {
        this.customerRepository = customerRepository;
        this.memoryService = memoryService;
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAllByOrderByLastInteractionDesc();
    }

    public Optional<Customer> findByCustomerId(String customerId) {
        return customerRepository.findByCustomerId(customerId);
    }

    public Optional<Customer> findByEmail(String email) {
        return customerRepository.findByEmail(email);
    }

    public Optional<Customer> findById(Long id) {
        return customerRepository.findById(id);
    }

    public List<Customer> searchCustomers(String query) {
        if (query == null || query.trim().isEmpty()) return getAllCustomers();
        return customerRepository.searchCustomers(query.trim());
    }

    @Transactional
    public Customer createCustomer(CustomerDto dto) {
        String customerId = dto.getCustomerId() != null ? dto.getCustomerId() : generateCustomerId();

        Customer.Plan plan = Customer.Plan.FREE;
        if (dto.getPlan() != null) {
            try { plan = Customer.Plan.valueOf(dto.getPlan()); } catch (Exception ignored) {}
        }

        Customer customer = Customer.builder()
                .customerId(customerId)
                .name(dto.getName())
                .email(dto.getEmail())
                .company(dto.getCompany())
                .phone(dto.getPhone())
                .plan(plan)
                .avatarColor(generateAvatarColor(dto.getName()))
                .customerSince(LocalDateTime.now())
                .build();

        customer = customerRepository.save(customer);

        String bankId = memoryService.initCustomerBank(customer);
        log.info("Customer created: {} with Hindsight bank: {}", customerId, bankId);

        memoryService.storeMemory(customer,
            String.format("%s signed up for the %s plan at %s",
                customer.getName(),
                customer.getPlan().name(),
                customer.getCompany() != null ? customer.getCompany() : "their company"),
            MemoryEntry.MemoryCategory.CUSTOMER_FACT,
            MemoryEntry.Importance.HIGH,
            null
        );

        return customer;
    }

    @Transactional
    public Customer updateLastInteraction(Customer customer) {
        customer.setLastInteraction(LocalDateTime.now());
        customer.setTotalConversations(customer.getTotalConversations() + 1);
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer updateSentiment(Customer customer, Customer.Sentiment sentiment) {
        customer.setCurrentSentiment(sentiment);
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer incrementOpenIssues(Customer customer) {
        customer.setOpenIssues(customer.getOpenIssues() + 1);
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer resolveIssue(Customer customer) {
        if (customer.getOpenIssues() > 0) {
            customer.setOpenIssues(customer.getOpenIssues() - 1);
        }
        customer.setResolvedIssues(customer.getResolvedIssues() + 1);
        return customerRepository.save(customer);
    }

    private String generateCustomerId() {
        long count = customerRepository.count() + 1;
        return String.format("CUST-%03d", count);
    }

    private String generateAvatarColor(String name) {
        String[] colors = {"#6366f1","#8b5cf6","#ec4899","#14b8a6","#f59e0b",
                           "#10b981","#3b82f6","#ef4444","#f97316","#06b6d4"};
        int idx = Math.abs((name != null ? name : "X").hashCode()) % colors.length;
        return colors[idx];
    }
}
