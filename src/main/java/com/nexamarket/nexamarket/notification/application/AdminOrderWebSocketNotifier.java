package com.nexamarket.nexamarket.notification.application;

import com.nexamarket.nexamarket.order.application.OrderStatusChangedEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/** Publishes the same durable order event to connected administrator screens. */
@Service
public class AdminOrderWebSocketNotifier {

    private static final String ADMIN_ORDER_TOPIC = "/topic/admin/orders";

    private final SimpMessagingTemplate messagingTemplate;

    public AdminOrderWebSocketNotifier(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publish(OrderStatusChangedEvent event) {
        messagingTemplate.convertAndSend(ADMIN_ORDER_TOPIC, event);
    }
}
