package com.recalldesk.app.dto;

import com.recalldesk.app.model.MemoryEntry;
import java.time.LocalDateTime;

public class MemoryDto {

    private Long id;
    private String customerId;
    private Long conversationId;
    private String category;
    private String categoryLabel;
    private String content;
    private String importance;
    private boolean resolved;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int retrievalCount;
    private String hindsightDocumentId;

    public MemoryDto() {}

    public static MemoryDto fromEntity(MemoryEntry m) {
        MemoryDto dto = new MemoryDto();
        dto.id = m.getId();
        dto.customerId = m.getCustomer().getCustomerId();
        dto.conversationId = m.getConversationId();
        dto.category = m.getCategory() != null ? m.getCategory().name() : "CUSTOM";
        dto.categoryLabel = formatCategory(m.getCategory());
        dto.content = m.getContent();
        dto.importance = m.getImportance() != null ? m.getImportance().name() : "MEDIUM";
        dto.resolved = m.getResolved();
        dto.createdAt = m.getCreatedAt();
        dto.updatedAt = m.getUpdatedAt();
        dto.retrievalCount = m.getRetrievalCount();
        dto.hindsightDocumentId = m.getHindsightDocumentId();
        return dto;
    }

    private static String formatCategory(MemoryEntry.MemoryCategory cat) {
        if (cat == null) return "Custom";
        return switch (cat) {
            case CUSTOMER_FACT -> "Customer Fact";
            case PREFERENCE -> "Preference";
            case PAST_PROBLEM -> "Past Problem";
            case RESOLUTION -> "Resolution";
            case UNRESOLVED_ISSUE -> "Unresolved Issue";
            case BEHAVIORAL -> "Behavioral";
            case SENTIMENT -> "Sentiment";
            case COMMITMENT -> "Commitment";
            case PRODUCT_USAGE -> "Product Usage";
            case CUSTOM -> "Custom";
        };
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getCategoryLabel() { return categoryLabel; }
    public void setCategoryLabel(String categoryLabel) { this.categoryLabel = categoryLabel; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getImportance() { return importance; }
    public void setImportance(String importance) { this.importance = importance; }

    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public int getRetrievalCount() { return retrievalCount; }
    public void setRetrievalCount(int retrievalCount) { this.retrievalCount = retrievalCount; }

    public String getHindsightDocumentId() { return hindsightDocumentId; }
    public void setHindsightDocumentId(String hindsightDocumentId) { this.hindsightDocumentId = hindsightDocumentId; }
}
