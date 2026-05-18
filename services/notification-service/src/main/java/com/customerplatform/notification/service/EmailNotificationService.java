package com.customerplatform.notification.service;

import com.customerplatform.notification.event.LoyaltyPointsAddedEvent;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    public EmailNotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendLoyaltyPointsEmail(LoyaltyPointsAddedEvent event) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("no-reply@customer-platform.local");
        message.setTo(event.email());
        message.setSubject("Vos points fidélité ont été mis à jour");
        message.setText("""
                Bonjour,

                Vous avez gagné %d points.
                Votre nouveau solde est de %d points.

                Merci pour votre fidélité.
                """.formatted(event.pointsAdded(), event.totalPoints()));

        mailSender.send(message);
    }
}
