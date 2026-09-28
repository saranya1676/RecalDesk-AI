package com.recalldesk.app.dto;

import com.recalldesk.app.model.Conversation;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class ConversationDto {

    private Long id;
    private String conversationId;
    private String customerId;
    private String customerName;
    private String subject;
    private String status;
    private String category;
    private String priority;
    private LocalDateTime createdAt;
    private LocalDateTime closedAt;
    private LocalDateTime lastMessageAt;
    private boolean hindsightUsed;
    private int memoriesRetrieved;
    private String resolution;
    private List<MessageDto> messages;

    public ConversationDto() {}

    public static ConversationDto fromEntity(Conversation c) {
        ConversationDto dto = new ConversationDto();
        dto.id = c.getId();
        dto.conversationId = c.getConversationId();
        dto.customerId = c.getCustomer().getCustomerId();
        dto.customerName = c.getCustomer().getName();
        dto.subject = c.getSubject();
        dto.status = c.getStatus() != null ? c.getStatus().name() : "OPEN";
        dto.category = c.getCategory() != null ? c.getCategory().name() : "GENERAL";
        dto.priority = c.getPriority() != null ? c.getPriority().name() : "MEDIUM";
        dto.createdAt = c.getCreatedAt();
        dto.closedAt = c.getClosedAt();
        dto.lastMessageAt = c.getLastMessageAt();
        dto.hindsightUsed = c.getHindsightUsed();
        dto.memoriesRetrieved = c.getMemoriesRetrieved();
        dto.resolution = c.getResolution();
        return dto;
    }

    public static ConversationDto fromEntityWithMessages(Conversation c) {
        ConversationDto dto = fromEntity(c);
        if (c.getMessages() != null) {
            dto.messages = c.getMessages().stream()
                    .map(MessageDto::fromEntity)
                    .collect(Collectors.toList());
        }
        return dto;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

    public LocalDateTime getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(LocalDateTime lastMessageAt) { this.lastMessageAt = lastMessageAt; }

    public boolean isHindsightUsed() { return hindsightUsed; }
    public void setHindsightUsed(boolean hindsightUsed) { this.hindsightUsed = hindsightUsed; }

    public int getMemoriesRetrieved() { return memoriesRetrieved; }
    public void setMemoriesRetrieved(int memoriesRetrieved) { this.memoriesRetrieved = memoriesRetrieved; }

    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }

    public List<MessageDto> getMessages() { return messages; }
    public void setMessages(List<MessageDto> messages) { this.messages = messages; }

    // ---- Nested ----
    public static class MessageDto {
        private Long id;
        private String role;
        private String content;
        private LocalDateTime createdAt;
        private boolean memoryUsed;
        private List<String> memoriesUsed;
        private String memoryImpactExplanation;
        private int memoriesCount;

        public MessageDto() {}

        public static MessageDto fromEntity(com.recalldesk.app.model.ChatMessage m) {
            MessageDto dto = new MessageDto();
            dto.id = m.getId();
            dto.role = m.getRole() != null ? m.getRole().name() : "USER";
            dto.content = m.getContent();
            dto.createdAt = m.getCreatedAt();
            dto.memoryUsed = m.getMemoryUsed();
            dto.memoriesCount = m.getMemoriesCount();
            dto.memoryImpactExplanation = m.getMemoryImpactExplanation();

            if (m.getMemoriesUsedJson() != null && !m.getMemoriesUsedJson().equals("[]")) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                    dto.memoriesUsed = om.readValue(m.getMemoriesUsedJson(),
                            om.getTypeFactory().constructCollectionType(List.class, String.class));
                } catch (Exception ignored) {}
            }
            return dto;
        }

        public Long getId() { return id; }
        public String getRole() { return role; }
        public String getContent() { return content; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public boolean isMemoryUsed() { return memoryUsed; }
        public List<String> getMemoriesUsed() { return memoriesUsed; }
        public String getMemoryImpactExplanation() { return memoryImpactExplanation; }
        public int getMemoriesCount() { return memoriesCount; }
    }
}
