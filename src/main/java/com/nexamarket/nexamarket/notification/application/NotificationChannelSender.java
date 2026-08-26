package com.nexamarket.nexamarket.notification.application;

import com.nexamarket.nexamarket.notification.domain.NotificationChannel;
import com.nexamarket.nexamarket.notification.domain.NotificationMessage;

public interface NotificationChannelSender {
    NotificationChannel channel();
    void send(NotificationMessage message);
}
