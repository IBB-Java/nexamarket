package com.nexamarket.nexamarket.notification.infrastructure;

import com.nexamarket.nexamarket.notification.application.NotificationChannelSender;
import com.nexamarket.nexamarket.notification.domain.NotificationChannel;
import com.nexamarket.nexamarket.notification.domain.NotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MockEmailNotificationSender implements NotificationChannelSender {
    private static final Logger log = LoggerFactory.getLogger(MockEmailNotificationSender.class);
    @Override public NotificationChannel channel() { return NotificationChannel.EMAIL; }
    @Override public void send(NotificationMessage message) { log.info("Mock e-mail notification sent: {}", message.getSubject()); }
}
