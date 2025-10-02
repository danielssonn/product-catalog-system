package com.bank.product.notification.domain.service.impl;

import com.bank.product.notification.domain.model.*;
import com.bank.product.notification.domain.repository.NotificationRepository;
import com.bank.product.notification.domain.service.NotificationDeliveryService;
import com.bank.product.notification.domain.service.NotificationService;
import com.bank.product.notification.domain.service.NotificationTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateService templateService;
    private final NotificationDeliveryService deliveryService;

    @Override
    public void handleWorkflowApproved(WorkflowApprovedEvent event) {
        log.info("Handling workflow approved event: workflowId={}", event.getWorkflowId());

        // Notify workflow initiator
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("workflowId", event.getWorkflowId());
        templateData.put("entityType", event.getEntityType());
        templateData.put("entityId", event.getEntityId());
        templateData.put("entityData", event.getEntityData());
        templateData.put("approvedAt", event.getApprovedAt());
        templateData.put("approvals", event.getApprovals());

        String message = templateService.generateMessage(
                NotificationType.WORKFLOW_APPROVED,
                templateData
        );

        Notification notification = Notification.builder()
                .id(UUID.randomUUID().toString())
                .workflowId(event.getWorkflowId())
                .entityType(event.getEntityType())
                .entityId(event.getEntityId())
                .tenantId(event.getTenantId())
                .type(NotificationType.WORKFLOW_APPROVED)
                .channel(NotificationChannel.EMAIL)
                .status(NotificationStatus.PENDING)
                .recipientEmail(event.getInitiatedBy())
                .subject("Workflow Approved: " + event.getEntityType())
                .message(message)
                .templateData(templateData)
                .createdAt(LocalDateTime.now())
                .retryCount(0)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Created notification: id={}", saved.getId());

        // Send notification asynchronously
        try {
            sendNotification(saved.getId());
        } catch (Exception e) {
            log.error("Error sending notification: id={}", saved.getId(), e);
        }
    }

    @Override
    public void handleWorkflowRejected(WorkflowRejectedEvent event) {
        log.info("Handling workflow rejected event: workflowId={}", event.getWorkflowId());

        Map<String, Object> templateData = new HashMap<>();
        templateData.put("workflowId", event.getWorkflowId());
        templateData.put("entityType", event.getEntityType());
        templateData.put("entityId", event.getEntityId());
        templateData.put("rejectedBy", event.getRejectedBy());
        templateData.put("rejectionReason", event.getRejectionReason());
        templateData.put("rejectionComments", event.getRejectionComments());
        templateData.put("rejectedAt", event.getRejectedAt());

        String message = templateService.generateMessage(
                NotificationType.WORKFLOW_REJECTED,
                templateData
        );

        Notification notification = Notification.builder()
                .id(UUID.randomUUID().toString())
                .workflowId(event.getWorkflowId())
                .entityType(event.getEntityType())
                .entityId(event.getEntityId())
                .tenantId(event.getTenantId())
                .type(NotificationType.WORKFLOW_REJECTED)
                .channel(NotificationChannel.EMAIL)
                .status(NotificationStatus.PENDING)
                .recipientEmail(event.getInitiatedBy())
                .subject("Workflow Rejected: " + event.getEntityType())
                .message(message)
                .templateData(templateData)
                .createdAt(LocalDateTime.now())
                .retryCount(0)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Created notification: id={}", saved.getId());

        try {
            sendNotification(saved.getId());
        } catch (Exception e) {
            log.error("Error sending notification: id={}", saved.getId(), e);
        }
    }

    @Override
    public Notification createNotification(Notification notification) {
        notification.setId(UUID.randomUUID().toString());
        notification.setCreatedAt(LocalDateTime.now());
        notification.setStatus(NotificationStatus.PENDING);
        return notificationRepository.save(notification);
    }

    @Override
    public Notification getNotificationById(String id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + id));
    }

    @Override
    public List<Notification> getNotificationsByWorkflowId(String workflowId) {
        return notificationRepository.findByWorkflowId(workflowId);
    }

    @Override
    public List<Notification> getNotificationsByRecipient(String recipientEmail) {
        return notificationRepository.findByRecipientEmailAndStatus(
                recipientEmail,
                NotificationStatus.SENT
        );
    }

    @Override
    public void sendNotification(String notificationId) {
        Notification notification = getNotificationById(notificationId);

        try {
            deliveryService.send(notification);

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notificationRepository.save(notification);

            log.info("Notification sent successfully: id={}", notificationId);
        } catch (Exception e) {
            log.error("Failed to send notification: id={}", notificationId, e);

            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
            notification.setRetryCount(notification.getRetryCount() + 1);
            notificationRepository.save(notification);

            throw e;
        }
    }

    @Override
    public void retryFailedNotifications() {
        List<Notification> failedNotifications = notificationRepository.findByStatus(NotificationStatus.FAILED);

        log.info("Retrying {} failed notifications", failedNotifications.size());

        for (Notification notification : failedNotifications) {
            if (notification.getRetryCount() < 3) {
                try {
                    sendNotification(notification.getId());
                } catch (Exception e) {
                    log.error("Retry failed for notification: id={}", notification.getId(), e);
                }
            }
        }
    }
}
