package com.customerplatform.notification.config;

import com.customerplatform.notification.event.LoyaltyPointsAddedEvent;
import com.customerplatform.notification.service.EmailNotificationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.messaging.MessageChannel;

@Configuration
public class NotificationIntegrationConfig {

    @Bean
    public MessageChannel loyaltyNotificationChannel() {
        return new DirectChannel();
    }

    @Bean
    public IntegrationFlow loyaltyNotificationFlow(
            EmailNotificationService emailNotificationService
    ) {
        return IntegrationFlow
                .from("loyaltyNotificationChannel")
                .handle(LoyaltyPointsAddedEvent.class, (payload, headers) -> {
                    emailNotificationService.sendLoyaltyPointsEmail(payload);
                    return null;
                })
                .get();
    }
}
