package com.recalldesk.app.repository;

import com.recalldesk.app.model.ChatMessage;
import com.recalldesk.app.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByConversationOrderByCreatedAtAsc(Conversation conversation);

    List<ChatMessage> findTop10ByConversationOrderByCreatedAtDesc(Conversation conversation);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.memoryUsed = true")
    long countPersonalizedResponses();

    @Query("SELECT SUM(m.memoriesCount) FROM ChatMessage m WHERE m.memoryUsed = true")
    Long sumTotalMemoriesUsed();
}
