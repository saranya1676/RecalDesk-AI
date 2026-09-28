package com.recalldesk.app.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String customerId;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    private String company;
    private String phone;

    @Enumerated(EnumType.STRING)
    private Plan plan = Plan.FREE;

    private String status = "ACTIVE";

    @Column(unique = true)
    private String hindsightBankId;

    private LocalDateTime customerSince = LocalDateTime.now();
    private LocalDateTime lastInteraction;

    @Enumerated(EnumType.STRING)
    private Sentiment currentSentiment = Sentiment.NEUTRAL;

    private Integer totalConversations = 0;
    private Integer resolvedIssues = 0;
    private Integer openIssues = 0;
    private Integer memoriesCount = 0;

    private String avatarColor;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Conversation> conversations;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MemoryEntry> memoryEntries;

    public enum Plan { FREE, STARTER, PRO, ENTERPRISE }
    public enum Sentiment { VERY_SATISFIED, SATISFIED, NEUTRAL, FRUSTRATED, VERY_FRUSTRATED }

    // ---- Constructors ----
    public Customer() {}

    // ---- Getters & Setters ----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Plan getPlan() { return plan; }
    public void setPlan(Plan plan) { this.plan = plan; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getHindsightBankId() { return hindsightBankId; }
    public void setHindsightBankId(String hindsightBankId) { this.hindsightBankId = hindsightBankId; }

    public LocalDateTime getCustomerSince() { return customerSince; }
    public void setCustomerSince(LocalDateTime customerSince) { this.customerSince = customerSince; }

    public LocalDateTime getLastInteraction() { return lastInteraction; }
    public void setLastInteraction(LocalDateTime lastInteraction) { this.lastInteraction = lastInteraction; }

    public Sentiment getCurrentSentiment() { return currentSentiment; }
    public void setCurrentSentiment(Sentiment currentSentiment) { this.currentSentiment = currentSentiment; }

    public Integer getTotalConversations() { return totalConversations != null ? totalConversations : 0; }
    public void setTotalConversations(Integer totalConversations) { this.totalConversations = totalConversations; }

    public Integer getResolvedIssues() { return resolvedIssues != null ? resolvedIssues : 0; }
    public void setResolvedIssues(Integer resolvedIssues) { this.resolvedIssues = resolvedIssues; }

    public Integer getOpenIssues() { return openIssues != null ? openIssues : 0; }
    public void setOpenIssues(Integer openIssues) { this.openIssues = openIssues; }

    public Integer getMemoriesCount() { return memoriesCount != null ? memoriesCount : 0; }
    public void setMemoriesCount(Integer memoriesCount) { this.memoriesCount = memoriesCount; }

    public String getAvatarColor() { return avatarColor; }
    public void setAvatarColor(String avatarColor) { this.avatarColor = avatarColor; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<Conversation> getConversations() { return conversations; }
    public void setConversations(List<Conversation> conversations) { this.conversations = conversations; }

    public List<MemoryEntry> getMemoryEntries() { return memoryEntries; }
    public void setMemoryEntries(List<MemoryEntry> memoryEntries) { this.memoryEntries = memoryEntries; }

    // ---- Builder ----
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final Customer c = new Customer();

        public Builder customerId(String v) { c.customerId = v; return this; }
        public Builder name(String v) { c.name = v; return this; }
        public Builder email(String v) { c.email = v; return this; }
        public Builder company(String v) { c.company = v; return this; }
        public Builder phone(String v) { c.phone = v; return this; }
        public Builder plan(Plan v) { c.plan = v; return this; }
        public Builder status(String v) { c.status = v; return this; }
        public Builder hindsightBankId(String v) { c.hindsightBankId = v; return this; }
        public Builder customerSince(LocalDateTime v) { c.customerSince = v; return this; }
        public Builder lastInteraction(LocalDateTime v) { c.lastInteraction = v; return this; }
        public Builder currentSentiment(Sentiment v) { c.currentSentiment = v; return this; }
        public Builder totalConversations(Integer v) { c.totalConversations = v; return this; }
        public Builder resolvedIssues(Integer v) { c.resolvedIssues = v; return this; }
        public Builder openIssues(Integer v) { c.openIssues = v; return this; }
        public Builder memoriesCount(Integer v) { c.memoriesCount = v; return this; }
        public Builder avatarColor(String v) { c.avatarColor = v; return this; }
        public Builder notes(String v) { c.notes = v; return this; }
        public Customer build() { return c; }
    }
}
