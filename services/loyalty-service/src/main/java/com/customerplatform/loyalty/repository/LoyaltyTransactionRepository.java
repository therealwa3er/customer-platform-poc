package com.customerplatform.loyalty.repository;

import com.customerplatform.loyalty.model.LoyaltyTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LoyaltyTransactionRepository extends JpaRepository<LoyaltyTransaction, UUID> {
    List<LoyaltyTransaction> findByCustomerIdOrderByCreatedAtDesc(String customerId);
}
