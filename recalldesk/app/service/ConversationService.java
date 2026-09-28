package com.recalldesk.app.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.recalldesk.app.model.Conversation;
import com.recalldesk.app.model.Customer;
import com.recalldesk.app.repository.ChatMessageRepository;
import com.recalldesk.app.repository.ConversationRepository;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;

    public ConversationService(ConversationRepository conversationRepository,
                               ChatMessageRepository chatMessageRepository) {
        this.conversationRepository = conversationRepository;
    }

    @Transactional
    public Conversation createConversation(Customer customer, String subject,
                                           Conversation.Category category) {
        String convId = "CONV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Conversation conv = Conversation.builder()
                .conversationId(convId)
                .customer(customer)
                .subject(subject != null ? subject : "Support Request")
                .category(category != null ? category : Conversation.Category.GENERAL)
                .status(Conversation.Status.OPEN)
                .createdAt(LocalDateTime.now())
                .lastMessageAt(LocalDateTime.now())
                .build();

        return conversationRepository.save(conv);
    }

    public Optional<Conversation> findByConversationId(String conversationId) {
        return conversationRepository.findByConversationId(conversationId);
    }

    public Optional<Conversation> findById(Long id) {
        return conversationRepository.findById(id);
    }

    public List<Conversation> getCustomerConversations(Customer customer) {
        return conversationRepository.findByCustomerOrderByCreatedAtDesc(customer);
    }

    @Transactional
    public Conversation resolveConversation(Conversation conversation, String resolution) {
        conversation.setStatus(Conversation.Status.RESOLVED);
        conversation.setClosedAt(LocalDateTime.now());
        conversation.setResolution(resolution);
        return conversationRepository.save(conversation);
    }

    @Transactional
    public Conversation closeConversation(Conversation conversation) {
        conversation.setStatus(Conversation.Status.CLOSED);
        conversation.setClosedAt(LocalDateTime.now());
        return conversationRepository.save(conversation);
    }

    public long getOpenConversationsCount() {
        return conversationRepository.countOpenConversations();
    }

    public long getResolvedConversationsCount() {
        return conversationRepository.countResolvedConversations();
    }
}
