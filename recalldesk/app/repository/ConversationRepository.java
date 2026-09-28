package com.recalldesk.app.repository;

import com.recalldesk.app.model.Conversation;
import com.recalldesk.app.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByConversationId(String conversationId);

    List<Conversation> findByCustomerOrderByCreatedAtDesc(Customer customer);

    List<Conversation> findByCustomerAndStatusOrderByCreatedAtDesc(
            Customer customer, Conversation.Status status);

    @Query("SELECT COUNT(c) FROM Conversation c WHERE c.status = 'OPEN' OR c.status = 'IN_PROGRESS'")
    long countOpenConversations();

    @Query("SELECT COUNT(c) FROM Conversation c WHERE c.status = 'RESOLVED'")
    long countResolvedConversations();

    @Query("SELECT COUNT(c) FROM Conversation c WHERE c.hindsightUsed = true")
    long countHindsightUsed();

    List<Conversation> findTop10ByOrderByLastMessageAtDesc();

    @Query("SELECT c FROM Conversation c WHERE c.customer.id = :customerId ORDER BY c.createdAt DESC")
    List<Conversation> findByCustomerIdOrdered(Long customerId);
}
