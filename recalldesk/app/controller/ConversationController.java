package com.recalldesk.app.controller;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.recalldesk.app.dto.ConversationDto;
import com.recalldesk.app.model.Conversation;
import com.recalldesk.app.repository.ChatMessageRepository;
import com.recalldesk.app.service.ConversationService;
import com.recalldesk.app.service.CustomerService;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;
    private final CustomerService customerService;
    private final ChatMessageRepository chatMessageRepository;

    public ConversationController(ConversationService conversationService,
                                  CustomerService customerService,
                                  ChatMessageRepository chatMessageRepository) {
        this.conversationService = conversationService;
        this.customerService = customerService;
        this.chatMessageRepository = chatMessageRepository;
    }

    @GetMapping("/{conversationId}")
    public ResponseEntity<?> getConversation(@PathVariable String conversationId) {
        return conversationService.findByConversationId(conversationId)
                .map(c -> {
                    var messages = chatMessageRepository.findByConversationOrderByCreatedAtAsc(c);
                    c.setMessages(messages);
                    return ResponseEntity.ok(ConversationDto.fromEntityWithMessages(c));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{conversationId}/resolve")
    public ResponseEntity<?> resolveConversation(@PathVariable String conversationId,
                                                  @RequestBody Map<String, String> body) {
        Optional<Conversation> convOpt = conversationService.findByConversationId(conversationId);
        if (convOpt.isEmpty()) return ResponseEntity.notFound().build();

        Conversation resolved = conversationService.resolveConversation(
                convOpt.get(), body.getOrDefault("resolution", "Issue resolved"));

        customerService.resolveIssue(resolved.getCustomer());
        return ResponseEntity.ok(ConversationDto.fromEntity(resolved));
    }

    @PostMapping("/{conversationId}/close")
    public ResponseEntity<?> closeConversation(@PathVariable String conversationId) {
        return conversationService.findByConversationId(conversationId)
                .map(c -> ResponseEntity.ok(
                        ConversationDto.fromEntity(conversationService.closeConversation(c))))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
