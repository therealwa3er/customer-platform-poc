package com.customerplatform.loyalty.repository;

import com.customerplatform.loyalty.model.LoyaltyAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LoyaltyAccountRepository extends JpaRepository<LoyaltyAccount, UUID> {
    Optional<LoyaltyAccount> findByCustomerId(String customerId);
}
