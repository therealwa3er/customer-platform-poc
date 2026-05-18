package com.customerplatform.loyalty.dto;

import com.customerplatform.loyalty.model.LoyaltyTier;

public record LoyaltyAccountResponse(
        String customerId,
        int points,
        LoyaltyTier tier
) {}
