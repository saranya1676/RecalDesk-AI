package com.recalldesk.app.dto;

import com.recalldesk.app.model.Customer;
import java.time.LocalDateTime;

public class CustomerDto {

    private Long id;
    private String customerId;
    private String name;
    private String email;
    private String company;
    private String phone;
    private String plan;
    private String status;
    private String hindsightBankId;
    private LocalDateTime customerSince;
    private LocalDateTime lastInteraction;
    private String currentSentiment;
    private int totalConversations;
    private int resolvedIssues;
    private int openIssues;
    private int memoriesCount;
    private String avatarColor;
    private String notes;

    public CustomerDto() {}

    public static CustomerDto fromEntity(Customer c) {
        CustomerDto dto = new CustomerDto();
        dto.id = c.getId();
        dto.customerId = c.getCustomerId();
        dto.name = c.getName();
        dto.email = c.getEmail();
        dto.company = c.getCompany();
        dto.phone = c.getPhone();
        dto.plan = c.getPlan() != null ? c.getPlan().name() : "FREE";
        dto.status = c.getStatus();
        dto.hindsightBankId = c.getHindsightBankId();
        dto.customerSince = c.getCustomerSince();
        dto.lastInteraction = c.getLastInteraction();
        dto.currentSentiment = c.getCurrentSentiment() != null ? c.getCurrentSentiment().name() : "NEUTRAL";
        dto.totalConversations = c.getTotalConversations();
        dto.resolvedIssues = c.getResolvedIssues();
        dto.openIssues = c.getOpenIssues();
        dto.memoriesCount = c.getMemoriesCount();
        dto.avatarColor = c.getAvatarColor();
        dto.notes = c.getNotes();
        return dto;
    }

    // Getters & Setters
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

    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getHindsightBankId() { return hindsightBankId; }
    public void setHindsightBankId(String hindsightBankId) { this.hindsightBankId = hindsightBankId; }

    public LocalDateTime getCustomerSince() { return customerSince; }
    public void setCustomerSince(LocalDateTime customerSince) { this.customerSince = customerSince; }

    public LocalDateTime getLastInteraction() { return lastInteraction; }
    public void setLastInteraction(LocalDateTime lastInteraction) { this.lastInteraction = lastInteraction; }

    public String getCurrentSentiment() { return currentSentiment; }
    public void setCurrentSentiment(String currentSentiment) { this.currentSentiment = currentSentiment; }

    public int getTotalConversations() { return totalConversations; }
    public void setTotalConversations(int totalConversations) { this.totalConversations = totalConversations; }

    public int getResolvedIssues() { return resolvedIssues; }
    public void setResolvedIssues(int resolvedIssues) { this.resolvedIssues = resolvedIssues; }

    public int getOpenIssues() { return openIssues; }
    public void setOpenIssues(int openIssues) { this.openIssues = openIssues; }

    public int getMemoriesCount() { return memoriesCount; }
    public void setMemoriesCount(int memoriesCount) { this.memoriesCount = memoriesCount; }

    public String getAvatarColor() { return avatarColor; }
    public void setAvatarColor(String avatarColor) { this.avatarColor = avatarColor; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
