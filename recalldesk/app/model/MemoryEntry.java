package com.recalldesk.app.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "memory_entries")
public class MemoryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    private Long conversationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemoryCategory category;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private String hindsightDocumentId;

    @Enumerated(EnumType.STRING)
    private Importance importance = Importance.MEDIUM;

    private Boolean resolved = false;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;
    private Integer retrievalCount = 0;

    public enum MemoryCategory {
        CUSTOMER_FACT, PREFERENCE, PAST_PROBLEM, RESOLUTION,
        UNRESOLVED_ISSUE, BEHAVIORAL, SENTIMENT, COMMITMENT,
        PRODUCT_USAGE, CUSTOM
    }

    public enum Importance { LOW, MEDIUM, HIGH, CRITICAL }

    public MemoryEntry() {}

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }

    public MemoryCategory getCategory() { return category; }
    public void setCategory(MemoryCategory category) { this.category = category; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getHindsightDocumentId() { return hindsightDocumentId; }
    public void setHindsightDocumentId(String hindsightDocumentId) { this.hindsightDocumentId = hindsightDocumentId; }

    public Importance getImportance() { return importance; }
    public void setImportance(Importance importance) { this.importance = importance; }

    public Boolean getResolved() { return resolved != null && resolved; }
    public void setResolved(Boolean resolved) { this.resolved = resolved; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Integer getRetrievalCount() { return retrievalCount != null ? retrievalCount : 0; }
    public void setRetrievalCount(Integer retrievalCount) { this.retrievalCount = retrievalCount; }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final MemoryEntry e = new MemoryEntry();

        public Builder customer(Customer v) { e.customer = v; return this; }
        public Builder conversationId(Long v) { e.conversationId = v; return this; }
        public Builder category(MemoryCategory v) { e.category = v; return this; }
        public Builder content(String v) { e.content = v; return this; }
        public Builder hindsightDocumentId(String v) { e.hindsightDocumentId = v; return this; }
        public Builder importance(Importance v) { e.importance = v; return this; }
        public Builder resolved(Boolean v) { e.resolved = v; return this; }
        public Builder createdAt(LocalDateTime v) { e.createdAt = v; return this; }
        public Builder updatedAt(LocalDateTime v) { e.updatedAt = v; return this; }
        public MemoryEntry build() { return e; }
    }
}
