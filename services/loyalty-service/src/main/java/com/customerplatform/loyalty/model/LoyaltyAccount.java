package com.customerplatform.loyalty.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "loyalty_accounts")
public class LoyaltyAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String customerId;

    private int points;

    @Enumerated(EnumType.STRING)
    private LoyaltyTier tier;

    public UUID getId() { return id; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
    public LoyaltyTier getTier() { return tier; }
    public void setTier(LoyaltyTier tier) { this.tier = tier; }
}
