package com.bank.product.notification.domain.repository;

import com.bank.product.notification.domain.model.Notification;
import com.bank.product.notification.domain.model.NotificationStatus;
import com.bank.product.notification.domain.model.NotificationType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {

    List<Notification> findByWorkflowId(String workflowId);

    List<Notification> findByEntityTypeAndEntityId(String entityType, String entityId);

    List<Notification> findByRecipientEmailAndStatus(String recipientEmail, NotificationStatus status);

    List<Notification> findByStatus(NotificationStatus status);

    List<Notification> findByTypeAndStatus(NotificationType type, NotificationStatus status);

    List<Notification> findByTenantId(String tenantId);
}
