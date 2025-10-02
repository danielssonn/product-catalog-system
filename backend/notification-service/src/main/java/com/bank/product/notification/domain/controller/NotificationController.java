package com.bank.product.notification.domain.controller;

import com.bank.product.notification.domain.model.Notification;
import com.bank.product.notification.domain.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/{id}")
    public ResponseEntity<Notification> getNotificationById(@PathVariable String id) {
        Notification notification = notificationService.getNotificationById(id);
        return ResponseEntity.ok(notification);
    }

    @GetMapping("/workflow/{workflowId}")
    public ResponseEntity<List<Notification>> getNotificationsByWorkflowId(@PathVariable String workflowId) {
        List<Notification> notifications = notificationService.getNotificationsByWorkflowId(workflowId);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/recipient/{email}")
    public ResponseEntity<List<Notification>> getNotificationsByRecipient(@PathVariable String email) {
        List<Notification> notifications = notificationService.getNotificationsByRecipient(email);
        return ResponseEntity.ok(notifications);
    }

    @PostMapping("/{id}/send")
    public ResponseEntity<Void> sendNotification(@PathVariable String id) {
        notificationService.sendNotification(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/retry-failed")
    public ResponseEntity<Void> retryFailedNotifications() {
        notificationService.retryFailedNotifications();
        return ResponseEntity.ok().build();
    }
}
