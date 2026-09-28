package com.recalldesk.app.repository;

import com.recalldesk.app.model.Customer;
import com.recalldesk.app.model.MemoryEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MemoryEntryRepository extends JpaRepository<MemoryEntry, Long> {

    List<MemoryEntry> findByCustomerOrderByCreatedAtDesc(Customer customer);

    List<MemoryEntry> findByCustomerAndCategoryOrderByCreatedAtDesc(
            Customer customer, MemoryEntry.MemoryCategory category);

    List<MemoryEntry> findByCustomerAndResolvedFalseOrderByImportanceDesc(Customer customer);

    @Query("SELECT COUNT(m) FROM MemoryEntry m")
    long countAllMemories();

    @Query("SELECT COUNT(m) FROM MemoryEntry m WHERE m.importance IN ('HIGH', 'CRITICAL')")
    long countImportantMemories();

    @Query("SELECT COUNT(m) FROM MemoryEntry m WHERE m.category = 'UNRESOLVED_ISSUE' AND m.resolved = false")
    long countOpenIssues();

    @Query("SELECT COUNT(m) FROM MemoryEntry m WHERE m.category = 'PREFERENCE'")
    long countPreferences();

    List<MemoryEntry> findTop20ByOrderByCreatedAtDesc();

    @Query("SELECT m FROM MemoryEntry m WHERE m.customer.id = :customerId ORDER BY m.createdAt DESC")
    List<MemoryEntry> findByCustomerIdOrdered(Long customerId);

    boolean existsByCustomerAndHindsightDocumentId(Customer customer, String hindsightDocumentId);
}
