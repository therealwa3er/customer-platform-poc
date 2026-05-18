package com.customerplatform.loyalty.dto;

public record LoyaltyPointsAddedEvent(
        String customerId,
        String email,
        int pointsAdded,
        int totalPoints
) {}
