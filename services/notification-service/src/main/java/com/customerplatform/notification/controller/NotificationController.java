package com.customerplatform.notification.controller;

import com.customerplatform.notification.event.LoyaltyPointsAddedEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final MessageChannel loyaltyNotificationChannel;

    public NotificationController(MessageChannel loyaltyNotificationChannel) {
        this.loyaltyNotificationChannel = loyaltyNotificationChannel;
    }

    @PostMapping("/loyalty-points")
    public Mono<ResponseEntity<Void>> notifyLoyaltyPoints(
            @RequestBody LoyaltyPointsAddedEvent event
    ) {
        loyaltyNotificationChannel.send(MessageBuilder.withPayload(event).build());
        return Mono.just(ResponseEntity.accepted().build());
    }
}
