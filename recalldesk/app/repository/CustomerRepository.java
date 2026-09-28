package com.recalldesk.app.repository;

import com.recalldesk.app.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByCustomerId(String customerId);

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByHindsightBankId(String hindsightBankId);

    @Query("SELECT c FROM Customer c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(c.company) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(c.customerId) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Customer> searchCustomers(String query);

    List<Customer> findAllByOrderByLastInteractionDesc();

    @Query("SELECT COUNT(c) FROM Customer c")
    long countTotalCustomers();

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.currentSentiment IN ('FRUSTRATED', 'VERY_FRUSTRATED')")
    long countFrustratedCustomers();
}
