package com.nexamarket.nexamarket.notification.infrastructure;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationMessagingConfiguration {
    public static final String EXCHANGE = "nexamarket.events";
    public static final String ORDER_STATUS_ROUTING_KEY = "order.status.changed";
    public static final String QUEUE = "nexamarket.notification.order-status";
    public static final String DELIVERY_STATUS_ROUTING_KEY = "delivery.status.changed";
    public static final String DELIVERY_QUEUE = "nexamarket.notification.delivery-status";
    @Bean DirectExchange nexamarketEventsExchange() { return new DirectExchange(EXCHANGE, true, false); }
    @Bean Queue orderStatusNotificationQueue() { return new Queue(QUEUE, true); }
    @Bean Binding orderStatusNotificationBinding(Queue orderStatusNotificationQueue, DirectExchange nexamarketEventsExchange) {
        return BindingBuilder.bind(orderStatusNotificationQueue).to(nexamarketEventsExchange).with(ORDER_STATUS_ROUTING_KEY);
    }
    @Bean Queue deliveryStatusNotificationQueue() { return new Queue(DELIVERY_QUEUE, true); }
    @Bean Binding deliveryStatusNotificationBinding(Queue deliveryStatusNotificationQueue,
                                                     DirectExchange nexamarketEventsExchange) {
        return BindingBuilder.bind(deliveryStatusNotificationQueue).to(nexamarketEventsExchange)
                .with(DELIVERY_STATUS_ROUTING_KEY);
    }
    @Bean MessageConverter messageConverter() { return new Jackson2JsonMessageConverter(); }
}
