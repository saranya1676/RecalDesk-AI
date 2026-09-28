package com.recalldesk.app.service;

import com.recalldesk.app.dto.DashboardDto;
import com.recalldesk.app.model.MemoryEntry;
import com.recalldesk.app.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class DashboardService {

    private final CustomerRepository customerRepository;
    private final ConversationRepository conversationRepository;
    private final MemoryEntryRepository memoryEntryRepository;
    private final ChatMessageRepository chatMessageRepository;

    public DashboardService(CustomerRepository customerRepository,
                            ConversationRepository conversationRepository,
                            MemoryEntryRepository memoryEntryRepository,
                            ChatMessageRepository chatMessageRepository) {
        this.customerRepository = customerRepository;
        this.conversationRepository = conversationRepository;
        this.memoryEntryRepository = memoryEntryRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    public DashboardDto getDashboard() {
        long totalMemories = memoryEntryRepository.countAllMemories();
        long importantMemories = memoryEntryRepository.countImportantMemories();
        long openIssues = memoryEntryRepository.countOpenIssues();
        long preferences = memoryEntryRepository.countPreferences();
        long personalizedResponses = chatMessageRepository.countPersonalizedResponses();

        return DashboardDto.builder()
                .totalCustomers(customerRepository.countTotalCustomers())
                .totalConversations(conversationRepository.count())
                .memoriesStored(totalMemories)
                .importantMemories(importantMemories)
                .resolvedIssues(conversationRepository.countResolvedConversations())
                .openIssues(openIssues)
                .personalizedResponses(personalizedResponses)
                .hindsightUsedConversations(conversationRepository.countHindsightUsed())
                .preferencesLearned(preferences)
                .frustrationCases(customerRepository.countFrustratedCustomers())
                .recentActivity(buildRecentActivity())
                .memoryGrowthData(buildMemoryGrowthData(totalMemories))
                .conversationData(buildConversationData())
                .dateLabels(buildDateLabels())
                .build();
    }

    private List<DashboardDto.RecentActivityDto> buildRecentActivity() {
        List<DashboardDto.RecentActivityDto> activities = new ArrayList<>();
        List<MemoryEntry> recentMemories = memoryEntryRepository.findTop20ByOrderByCreatedAtDesc();

        for (MemoryEntry m : recentMemories) {
            String icon = getIconForCategory(m.getCategory());
            String message = getMessageForCategory(m.getCategory(), m.getContent());
            String timeAgo = formatTimeAgo(m.getCreatedAt());

            activities.add(DashboardDto.RecentActivityDto.builder()
                    .icon(icon)
                    .message(message)
                    .customerName(m.getCustomer().getName())
                    .customerId(m.getCustomer().getCustomerId())
                    .timeAgo(timeAgo)
                    .type(m.getCategory() != null ? m.getCategory().name().toLowerCase() : "custom")
                    .build());
        }
        return activities;
    }

    private List<Integer> buildMemoryGrowthData(long total) {
        List<Integer> data = new ArrayList<>();
        long base = Math.max(0, total - 30);
        Random rng = new Random(42);
        for (int i = 6; i >= 0; i--) {
            data.add((int) (base + (total - base) * ((7 - i) / 7.0) + rng.nextInt(3)));
        }
        return data;
    }

    private List<Integer> buildConversationData() {
        List<Integer> data = new ArrayList<>();
        long total = conversationRepository.count();
        Random rng = new Random(123);
        for (int i = 0; i < 7; i++) {
            data.add((int) (total / 7) + rng.nextInt(3));
        }
        return data;
    }

    private List<String> buildDateLabels() {
        List<String> labels = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d");
        LocalDateTime now = LocalDateTime.now();
        for (int i = 6; i >= 0; i--) {
            labels.add(now.minusDays(i).format(fmt));
        }
        return labels;
    }

    private String getIconForCategory(MemoryEntry.MemoryCategory cat) {
        if (cat == null) return "bi-bookmark";
        return switch (cat) {
            case PREFERENCE -> "bi-heart";
            case PAST_PROBLEM -> "bi-exclamation-triangle";
            case RESOLUTION -> "bi-check-circle";
            case UNRESOLVED_ISSUE -> "bi-clock-history";
            case SENTIMENT -> "bi-emoji-smile";
            case COMMITMENT -> "bi-shield-check";
            case CUSTOMER_FACT -> "bi-person";
            case BEHAVIORAL -> "bi-graph-up";
            case PRODUCT_USAGE -> "bi-cpu";
            default -> "bi-bookmark";
        };
    }

    private String getMessageForCategory(MemoryEntry.MemoryCategory cat, String content) {
        String snippet = content != null
                ? (content.length() > 50 ? content.substring(0, 50) + "…" : content)
                : "";
        if (cat == null) return "Memory stored: " + snippet;
        return switch (cat) {
            case PREFERENCE -> "Preference learned: " + snippet;
            case PAST_PROBLEM -> "Issue recorded: " + snippet;
            case RESOLUTION -> "Issue resolved: " + snippet;
            case UNRESOLVED_ISSUE -> "Open issue: " + snippet;
            case SENTIMENT -> "Sentiment noted: " + snippet;
            case COMMITMENT -> "Commitment stored: " + snippet;
            default -> "Memory stored: " + snippet;
        };
    }

    private String formatTimeAgo(LocalDateTime dt) {
        if (dt == null) return "just now";
        long minutes = java.time.Duration.between(dt, LocalDateTime.now()).toMinutes();
        if (minutes < 1) return "just now";
        if (minutes < 60) return minutes + "m ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h ago";
        return (hours / 24) + "d ago";
    }
}
