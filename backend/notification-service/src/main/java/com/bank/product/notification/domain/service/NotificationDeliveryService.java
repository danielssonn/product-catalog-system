package com.bank.product.notification.domain.service;

import com.bank.product.notification.domain.model.Notification;

public interface NotificationDeliveryService {
    void send(Notification notification);
}
