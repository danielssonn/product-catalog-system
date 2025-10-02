package com.bank.product.notification.domain.service;

import com.bank.product.notification.domain.model.Notification;
import com.bank.product.notification.domain.model.WorkflowApprovedEvent;
import com.bank.product.notification.domain.model.WorkflowRejectedEvent;

import java.util.List;

public interface NotificationService {

    void handleWorkflowApproved(WorkflowApprovedEvent event);

    void handleWorkflowRejected(WorkflowRejectedEvent event);

    Notification createNotification(Notification notification);

    Notification getNotificationById(String id);

    List<Notification> getNotificationsByWorkflowId(String workflowId);

    List<Notification> getNotificationsByRecipient(String recipientEmail);

    void sendNotification(String notificationId);

    void retryFailedNotifications();
}
