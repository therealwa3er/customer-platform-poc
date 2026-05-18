package com.customerplatform.notification.event;

public record LoyaltyPointsAddedEvent(
        String customerId,
        String email,
        int pointsAdded,
        int totalPoints
) {}
