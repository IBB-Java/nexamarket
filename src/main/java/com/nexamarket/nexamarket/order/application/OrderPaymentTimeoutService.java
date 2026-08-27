package com.nexamarket.nexamarket.order.application;

import com.nexamarket.nexamarket.order.domain.CustomerOrder;
import com.nexamarket.nexamarket.order.domain.OrderStateMachine;
import com.nexamarket.nexamarket.order.domain.OrderStatus;
import com.nexamarket.nexamarket.order.domain.SubOrder;
import com.nexamarket.nexamarket.order.infrastructure.CustomerOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

@Service
public class OrderPaymentTimeoutService {

    private final CustomerOrderRepository customerOrderRepository;
    private final StockReservationReleaseGateway stockReservationReleaseGateway;
    private final OrderStateMachine orderStateMachine;
    private final Clock clock;
    private final Duration paymentTimeout;
    private final OrderStatusEventPublisher orderStatusEventPublisher;

    @Autowired
    public OrderPaymentTimeoutService(CustomerOrderRepository customerOrderRepository,
                                      StockReservationReleaseGateway stockReservationReleaseGateway,
                                      OrderStatusEventPublisher orderStatusEventPublisher,
                                      @Value("${order.payment-timeout}") Duration paymentTimeout) {
        this(customerOrderRepository, stockReservationReleaseGateway, orderStatusEventPublisher, paymentTimeout, Clock.systemUTC());
    }

    OrderPaymentTimeoutService(CustomerOrderRepository customerOrderRepository,
                               StockReservationReleaseGateway stockReservationReleaseGateway,
                               Duration paymentTimeout, Clock clock) {
        this(customerOrderRepository, stockReservationReleaseGateway, null, paymentTimeout, clock);
    }

    OrderPaymentTimeoutService(CustomerOrderRepository customerOrderRepository,
                               StockReservationReleaseGateway stockReservationReleaseGateway,
                               OrderStatusEventPublisher orderStatusEventPublisher,
                               Duration paymentTimeout, Clock clock) {
        this.customerOrderRepository = customerOrderRepository;
        this.stockReservationReleaseGateway = stockReservationReleaseGateway;
        this.orderStateMachine = new OrderStateMachine();
        this.paymentTimeout = paymentTimeout;
        this.clock = clock;
        this.orderStatusEventPublisher = orderStatusEventPublisher;
    }

    @Scheduled(fixedDelayString = "${order.payment-timeout.check-interval-ms}")
    @Transactional
    public void cancelTimedOutOrdersOnSchedule() {
        cancelTimedOutOrders();
    }

    /**
     * Reservations are released before state changes. A release error leaves the
     * order untouched so the next scheduled run can retry safely.
     */
    @Transactional
    public int cancelTimedOutOrders() {
        Instant createdBefore = Instant.now(clock).minus(paymentTimeout);
        List<CustomerOrder> timedOutOrders = customerOrderRepository.findByStatusAndCreatedAtBeforeForUpdate(
                OrderStatus.PAYMENT_PENDING, createdBefore);

        for (CustomerOrder order : timedOutOrders) {
            releaseReservations(order);
            cancel(order);
        }
        return timedOutOrders.size();
    }

    private void releaseReservations(CustomerOrder order) {
        Set<String> releasedCodes = new HashSet<>();
        for (SubOrder subOrder : order.getSubOrders()) {
            subOrder.getItems().forEach(item -> {
                if (releasedCodes.add(item.getStockReservationCode())) {
                    stockReservationReleaseGateway.releaseReservation(item.getStockReservationCode());
                }
            });
        }
    }

    private void cancel(CustomerOrder order) {
        for (SubOrder subOrder : order.getSubOrders()) {
            orderStateMachine.transition(subOrder, OrderStatus.CANCELLED);
            if (orderStatusEventPublisher != null) {
                orderStatusEventPublisher.enqueue(new OrderStatusChangedEvent(java.util.UUID.randomUUID(),
                        order.getCustomerId(), subOrder.getId(), subOrder.getSellerId(), subOrder.getStatus()));
            }
        }
        orderStateMachine.transition(order, OrderStatus.CANCELLED);
    }
}
