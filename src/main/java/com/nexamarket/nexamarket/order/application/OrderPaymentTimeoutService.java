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

@Service
public class OrderPaymentTimeoutService {

    private final CustomerOrderRepository customerOrderRepository;
    private final StockReservationReleaseGateway stockReservationReleaseGateway;
    private final OrderStateMachine orderStateMachine;
    private final Clock clock;
    private final Duration paymentTimeout;

    @Autowired
    public OrderPaymentTimeoutService(CustomerOrderRepository customerOrderRepository,
                                      StockReservationReleaseGateway stockReservationReleaseGateway,
                                      @Value("${order.payment-timeout}") Duration paymentTimeout) {
        this(customerOrderRepository, stockReservationReleaseGateway, paymentTimeout, Clock.systemUTC());
    }

    OrderPaymentTimeoutService(CustomerOrderRepository customerOrderRepository,
                               StockReservationReleaseGateway stockReservationReleaseGateway,
                               Duration paymentTimeout, Clock clock) {
        this.customerOrderRepository = customerOrderRepository;
        this.stockReservationReleaseGateway = stockReservationReleaseGateway;
        this.orderStateMachine = new OrderStateMachine();
        this.paymentTimeout = paymentTimeout;
        this.clock = clock;
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
        for (SubOrder subOrder : order.getSubOrders()) {
            subOrder.getItems().forEach(item -> stockReservationReleaseGateway.releaseReservation(item.getStockReservationCode()));
        }
    }

    private void cancel(CustomerOrder order) {
        for (SubOrder subOrder : order.getSubOrders()) {
            orderStateMachine.transition(subOrder, OrderStatus.CANCELLED);
        }
        orderStateMachine.transition(order, OrderStatus.CANCELLED);
    }
}
