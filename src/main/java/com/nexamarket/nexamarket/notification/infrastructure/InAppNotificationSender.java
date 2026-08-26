package com.nexamarket.nexamarket.notification.infrastructure;

import com.nexamarket.nexamarket.notification.application.NotificationChannelSender;
import com.nexamarket.nexamarket.notification.domain.NotificationChannel;
import com.nexamarket.nexamarket.notification.domain.NotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InAppNotificationSender implements NotificationChannelSender {
    private static final Logger log = LoggerFactory.getLogger(InAppNotificationSender.class);
    @Override public NotificationChannel channel() { return NotificationChannel.IN_APP; }
    @Override public void send(NotificationMessage message) { log.info("In-app notification persisted for delivery: {}", message.getContent()); }
}
