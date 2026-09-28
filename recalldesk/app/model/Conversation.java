package com.recalldesk.app.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "conversations")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String conversationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    private String subject;

    @Enumerated(EnumType.STRING)
    private Status status = Status.OPEN;

    @Enumerated(EnumType.STRING)
    private Category category = Category.GENERAL;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime closedAt;
    private LocalDateTime lastMessageAt;

    private Boolean hindsightUsed = false;
    private Integer memoriesRetrieved = 0;

    @Column(columnDefinition = "TEXT")
    private String memoryContextSnapshot;

    @Column(columnDefinition = "TEXT")
    private String resolution;

    @Enumerated(EnumType.STRING)
    private Priority priority = Priority.MEDIUM;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    private List<ChatMessage> messages;

    public enum Status { OPEN, IN_PROGRESS, RESOLVED, CLOSED }
    public enum Category { BILLING, TECHNICAL, ACCOUNT, PRODUCT, GENERAL, REFUND, ONBOARDING }
    public enum Priority { LOW, MEDIUM, HIGH, URGENT }

    public Conversation() {}

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

    public LocalDateTime getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(LocalDateTime lastMessageAt) { this.lastMessageAt = lastMessageAt; }

    public Boolean getHindsightUsed() { return hindsightUsed != null && hindsightUsed; }
    public void setHindsightUsed(Boolean hindsightUsed) { this.hindsightUsed = hindsightUsed; }

    public Integer getMemoriesRetrieved() { return memoriesRetrieved != null ? memoriesRetrieved : 0; }
    public void setMemoriesRetrieved(Integer memoriesRetrieved) { this.memoriesRetrieved = memoriesRetrieved; }

    public String getMemoryContextSnapshot() { return memoryContextSnapshot; }
    public void setMemoryContextSnapshot(String memoryContextSnapshot) { this.memoryContextSnapshot = memoryContextSnapshot; }

    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    public List<ChatMessage> getMessages() { return messages; }
    public void setMessages(List<ChatMessage> messages) { this.messages = messages; }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final Conversation c = new Conversation();

        public Builder conversationId(String v) { c.conversationId = v; return this; }
        public Builder customer(Customer v) { c.customer = v; return this; }
        public Builder subject(String v) { c.subject = v; return this; }
        public Builder status(Status v) { c.status = v; return this; }
        public Builder category(Category v) { c.category = v; return this; }
        public Builder priority(Priority v) { c.priority = v; return this; }
        public Builder createdAt(LocalDateTime v) { c.createdAt = v; return this; }
        public Builder closedAt(LocalDateTime v) { c.closedAt = v; return this; }
        public Builder lastMessageAt(LocalDateTime v) { c.lastMessageAt = v; return this; }
        public Builder hindsightUsed(Boolean v) { c.hindsightUsed = v; return this; }
        public Builder memoriesRetrieved(Integer v) { c.memoriesRetrieved = v; return this; }
        public Builder resolution(String v) { c.resolution = v; return this; }
        public Conversation build() { return c; }
    }
}
