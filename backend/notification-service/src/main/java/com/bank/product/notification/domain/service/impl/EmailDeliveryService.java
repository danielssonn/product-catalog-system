package com.bank.product.notification.domain.service.impl;

import com.bank.product.notification.domain.model.Notification;
import com.bank.product.notification.domain.service.NotificationDeliveryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailDeliveryService implements NotificationDeliveryService {

    @Override
    public void send(Notification notification) {
        // In a real implementation, this would integrate with an email service like SendGrid, AWS SES, etc.
        // For now, we'll just log the email details

        log.info("""

                ================== EMAIL NOTIFICATION ==================
                To: {}
                Subject: {}

                {}
                ========================================================
                """,
                notification.getRecipientEmail(),
                notification.getSubject(),
                notification.getMessage()
        );

        // Simulate email sending
        log.info("Email sent successfully to: {}", notification.getRecipientEmail());
    }
}
