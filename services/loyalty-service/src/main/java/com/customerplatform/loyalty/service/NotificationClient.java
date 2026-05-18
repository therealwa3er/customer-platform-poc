package com.customerplatform.loyalty.service;

import com.customerplatform.loyalty.dto.LoyaltyPointsAddedEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class NotificationClient {

    private final RestClient notificationRestClient;

    public NotificationClient(RestClient notificationRestClient) {
        this.notificationRestClient = notificationRestClient;
    }

    public void sendLoyaltyPointsEmail(LoyaltyPointsAddedEvent event) {
        notificationRestClient.post()
                .uri("/api/notifications/loyalty-points")
                .body(event)
                .retrieve()
                .toBodilessEntity();
    }
}
