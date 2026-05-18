package com.customerplatform.loyalty.service;

import com.customerplatform.loyalty.dto.AddPointsRequest;
import com.customerplatform.loyalty.dto.LoyaltyAccountResponse;
import com.customerplatform.loyalty.dto.LoyaltyPointsAddedEvent;
import com.customerplatform.loyalty.model.LoyaltyAccount;
import com.customerplatform.loyalty.model.LoyaltyTier;
import com.customerplatform.loyalty.model.LoyaltyTransaction;
import com.customerplatform.loyalty.repository.LoyaltyAccountRepository;
import com.customerplatform.loyalty.repository.LoyaltyTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class LoyaltyService {

    private final LoyaltyAccountRepository accountRepository;
    private final LoyaltyTransactionRepository transactionRepository;
    private final NotificationClient notificationClient;

    public LoyaltyService(
            LoyaltyAccountRepository accountRepository,
            LoyaltyTransactionRepository transactionRepository,
            NotificationClient notificationClient
    ) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.notificationClient = notificationClient;
    }

    @Transactional
    public LoyaltyAccountResponse addPoints(String customerId, AddPointsRequest request) {
        LoyaltyAccount account = accountRepository.findByCustomerId(customerId)
                .orElseGet(() -> createAccount(customerId));

        account.setPoints(account.getPoints() + request.points());
        account.setTier(calculateTier(account.getPoints()));

        LoyaltyTransaction transaction = new LoyaltyTransaction();
        transaction.setCustomerId(customerId);
        transaction.setPoints(request.points());
        transaction.setReason(request.reason());
        transaction.setCreatedAt(LocalDateTime.now());

        transactionRepository.save(transaction);
        accountRepository.save(account);

        notificationClient.sendLoyaltyPointsEmail(
                new LoyaltyPointsAddedEvent(
                        customerId,
                        request.email(),
                        request.points(),
                        account.getPoints()
                )
        );

        return new LoyaltyAccountResponse(
                account.getCustomerId(),
                account.getPoints(),
                account.getTier()
        );
    }

    public LoyaltyAccountResponse getAccount(String customerId) {
        LoyaltyAccount account = accountRepository.findByCustomerId(customerId)
                .orElseGet(() -> createAccount(customerId));

        return new LoyaltyAccountResponse(
                account.getCustomerId(),
                account.getPoints(),
                account.getTier()
        );
    }

    private LoyaltyAccount createAccount(String customerId) {
        LoyaltyAccount account = new LoyaltyAccount();
        account.setCustomerId(customerId);
        account.setPoints(0);
        account.setTier(LoyaltyTier.BRONZE);
        return accountRepository.save(account);
    }

    private LoyaltyTier calculateTier(int points) {
        if (points >= 5000) return LoyaltyTier.GOLD;
        if (points >= 1000) return LoyaltyTier.SILVER;
        return LoyaltyTier.BRONZE;
    }
}
