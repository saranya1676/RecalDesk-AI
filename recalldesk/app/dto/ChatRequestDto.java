package com.recalldesk.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChatRequestDto {

    @NotBlank(message = "Message cannot be empty")
    @Size(max = 2000, message = "Message too long (max 2000 characters)")
    private String message;

    @NotBlank(message = "Customer identifier required")
    private String customerId;

    private String conversationId;
    private String subject;
    private String category;

    public ChatRequestDto() {}

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
