package com.recalldesk.app.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private LocalDateTime createdAt = LocalDateTime.now();
    private Boolean memoryUsed = false;

    @Column(columnDefinition = "TEXT")
    private String memoriesUsedJson;

    @Column(columnDefinition = "TEXT")
    private String memoryImpactExplanation;

    private Integer memoriesCount = 0;

    public enum Role { USER, AGENT, SYSTEM }

    public ChatMessage() {}

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Conversation getConversation() { return conversation; }
    public void setConversation(Conversation conversation) { this.conversation = conversation; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Boolean getMemoryUsed() { return memoryUsed != null && memoryUsed; }
    public void setMemoryUsed(Boolean memoryUsed) { this.memoryUsed = memoryUsed; }

    public String getMemoriesUsedJson() { return memoriesUsedJson; }
    public void setMemoriesUsedJson(String memoriesUsedJson) { this.memoriesUsedJson = memoriesUsedJson; }

    public String getMemoryImpactExplanation() { return memoryImpactExplanation; }
    public void setMemoryImpactExplanation(String memoryImpactExplanation) { this.memoryImpactExplanation = memoryImpactExplanation; }

    public Integer getMemoriesCount() { return memoriesCount != null ? memoriesCount : 0; }
    public void setMemoriesCount(Integer memoriesCount) { this.memoriesCount = memoriesCount; }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final ChatMessage m = new ChatMessage();

        public Builder conversation(Conversation v) { m.conversation = v; return this; }
        public Builder role(Role v) { m.role = v; return this; }
        public Builder content(String v) { m.content = v; return this; }
        public Builder createdAt(LocalDateTime v) { m.createdAt = v; return this; }
        public Builder memoryUsed(Boolean v) { m.memoryUsed = v; return this; }
        public Builder memoriesUsedJson(String v) { m.memoriesUsedJson = v; return this; }
        public Builder memoryImpactExplanation(String v) { m.memoryImpactExplanation = v; return this; }
        public Builder memoriesCount(Integer v) { m.memoriesCount = v; return this; }
        public ChatMessage build() { return m; }
    }
}
