package com.recalldesk.app.dto;

import java.util.List;

public class ChatResponseDto {

    private Long messageId;
    private String content;
    private String conversationId;
    private List<String> memoriesUsed;
    private int memoriesCount;
    private String memoryImpactExplanation;
    private boolean hindsightAvailable;
    private boolean memoryUsed;
    private boolean success;
    private String errorMessage;
    private String customerName;
    private String customerPlan;

    public ChatResponseDto() {}

    // Getters & Setters
    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public List<String> getMemoriesUsed() { return memoriesUsed; }
    public void setMemoriesUsed(List<String> memoriesUsed) { this.memoriesUsed = memoriesUsed; }

    public int getMemoriesCount() { return memoriesCount; }
    public void setMemoriesCount(int memoriesCount) { this.memoriesCount = memoriesCount; }

    public String getMemoryImpactExplanation() { return memoryImpactExplanation; }
    public void setMemoryImpactExplanation(String memoryImpactExplanation) { this.memoryImpactExplanation = memoryImpactExplanation; }

    public boolean isHindsightAvailable() { return hindsightAvailable; }
    public void setHindsightAvailable(boolean hindsightAvailable) { this.hindsightAvailable = hindsightAvailable; }

    public boolean isMemoryUsed() { return memoryUsed; }
    public void setMemoryUsed(boolean memoryUsed) { this.memoryUsed = memoryUsed; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPlan() { return customerPlan; }
    public void setCustomerPlan(String customerPlan) { this.customerPlan = customerPlan; }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final ChatResponseDto d = new ChatResponseDto();

        public Builder messageId(Long v) { d.messageId = v; return this; }
        public Builder content(String v) { d.content = v; return this; }
        public Builder conversationId(String v) { d.conversationId = v; return this; }
        public Builder memoriesUsed(List<String> v) { d.memoriesUsed = v; return this; }
        public Builder memoriesCount(int v) { d.memoriesCount = v; return this; }
        public Builder memoryImpactExplanation(String v) { d.memoryImpactExplanation = v; return this; }
        public Builder hindsightAvailable(boolean v) { d.hindsightAvailable = v; return this; }
        public Builder memoryUsed(boolean v) { d.memoryUsed = v; return this; }
        public Builder success(boolean v) { d.success = v; return this; }
        public Builder errorMessage(String v) { d.errorMessage = v; return this; }
        public ChatResponseDto build() { return d; }
    }
}
