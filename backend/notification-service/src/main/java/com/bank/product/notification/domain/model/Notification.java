package com.bank.product.notification.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notifications")
public class Notification {

    @Id
    private String id;

    private String workflowId;
    private String entityType;
    private String entityId;
    private String tenantId;

    private NotificationType type;
    private NotificationChannel channel;
    private NotificationStatus status;

    private String recipientEmail;
    private String recipientName;
    private String recipientRole;

    private String subject;
    private String message;
    private Map<String, Object> templateData;

    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime readAt;

    private String errorMessage;
    private int retryCount;
}
