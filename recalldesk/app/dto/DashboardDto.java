package com.recalldesk.app.dto;

import java.util.List;

public class DashboardDto {

    private long totalCustomers;
    private long totalConversations;
    private long memoriesStored;
    private long importantMemories;
    private long resolvedIssues;
    private long openIssues;
    private long personalizedResponses;
    private long hindsightUsedConversations;
    private long preferencesLearned;
    private long frustrationCases;
    private List<RecentActivityDto> recentActivity;
    private List<Integer> memoryGrowthData;
    private List<Integer> conversationData;
    private List<String> dateLabels;

    public DashboardDto() {}

    // Getters & Setters
    public long getTotalCustomers() { return totalCustomers; }
    public void setTotalCustomers(long v) { this.totalCustomers = v; }

    public long getTotalConversations() { return totalConversations; }
    public void setTotalConversations(long v) { this.totalConversations = v; }

    public long getMemoriesStored() { return memoriesStored; }
    public void setMemoriesStored(long v) { this.memoriesStored = v; }

    public long getImportantMemories() { return importantMemories; }
    public void setImportantMemories(long v) { this.importantMemories = v; }

    public long getResolvedIssues() { return resolvedIssues; }
    public void setResolvedIssues(long v) { this.resolvedIssues = v; }

    public long getOpenIssues() { return openIssues; }
    public void setOpenIssues(long v) { this.openIssues = v; }

    public long getPersonalizedResponses() { return personalizedResponses; }
    public void setPersonalizedResponses(long v) { this.personalizedResponses = v; }

    public long getHindsightUsedConversations() { return hindsightUsedConversations; }
    public void setHindsightUsedConversations(long v) { this.hindsightUsedConversations = v; }

    public long getPreferencesLearned() { return preferencesLearned; }
    public void setPreferencesLearned(long v) { this.preferencesLearned = v; }

    public long getFrustrationCases() { return frustrationCases; }
    public void setFrustrationCases(long v) { this.frustrationCases = v; }

    public List<RecentActivityDto> getRecentActivity() { return recentActivity; }
    public void setRecentActivity(List<RecentActivityDto> v) { this.recentActivity = v; }

    public List<Integer> getMemoryGrowthData() { return memoryGrowthData; }
    public void setMemoryGrowthData(List<Integer> v) { this.memoryGrowthData = v; }

    public List<Integer> getConversationData() { return conversationData; }
    public void setConversationData(List<Integer> v) { this.conversationData = v; }

    public List<String> getDateLabels() { return dateLabels; }
    public void setDateLabels(List<String> v) { this.dateLabels = v; }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final DashboardDto d = new DashboardDto();

        public Builder totalCustomers(long v) { d.totalCustomers = v; return this; }
        public Builder totalConversations(long v) { d.totalConversations = v; return this; }
        public Builder memoriesStored(long v) { d.memoriesStored = v; return this; }
        public Builder importantMemories(long v) { d.importantMemories = v; return this; }
        public Builder resolvedIssues(long v) { d.resolvedIssues = v; return this; }
        public Builder openIssues(long v) { d.openIssues = v; return this; }
        public Builder personalizedResponses(long v) { d.personalizedResponses = v; return this; }
        public Builder hindsightUsedConversations(long v) { d.hindsightUsedConversations = v; return this; }
        public Builder preferencesLearned(long v) { d.preferencesLearned = v; return this; }
        public Builder frustrationCases(long v) { d.frustrationCases = v; return this; }
        public Builder recentActivity(List<RecentActivityDto> v) { d.recentActivity = v; return this; }
        public Builder memoryGrowthData(List<Integer> v) { d.memoryGrowthData = v; return this; }
        public Builder conversationData(List<Integer> v) { d.conversationData = v; return this; }
        public Builder dateLabels(List<String> v) { d.dateLabels = v; return this; }
        public DashboardDto build() { return d; }
    }

    // ---- Nested DTO ----
    public static class RecentActivityDto {
        private String icon;
        private String message;
        private String customerName;
        private String customerId;
        private String timeAgo;
        private String type;

        public RecentActivityDto() {}

        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }
        public String getTimeAgo() { return timeAgo; }
        public void setTimeAgo(String timeAgo) { this.timeAgo = timeAgo; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private final RecentActivityDto d = new RecentActivityDto();
            public Builder icon(String v) { d.icon = v; return this; }
            public Builder message(String v) { d.message = v; return this; }
            public Builder customerName(String v) { d.customerName = v; return this; }
            public Builder customerId(String v) { d.customerId = v; return this; }
            public Builder timeAgo(String v) { d.timeAgo = v; return this; }
            public Builder type(String v) { d.type = v; return this; }
            public RecentActivityDto build() { return d; }
        }
    }
}
