package com.nexamarket.nexamarket.order.application;

import com.nexamarket.nexamarket.order.domain.OrderStateMachine;
import com.nexamarket.nexamarket.order.domain.OrderStatus;
import com.nexamarket.nexamarket.order.domain.SubOrder;
import com.nexamarket.nexamarket.order.infrastructure.SubOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class OrderStatusService {

    private final SubOrderRepository subOrderRepository;
    private final OrderStateMachine orderStateMachine;
    private final OrderStatusEventPublisher orderStatusEventPublisher;

    public OrderStatusService(SubOrderRepository subOrderRepository, OrderStatusEventPublisher orderStatusEventPublisher) {
        this.subOrderRepository = subOrderRepository;
        this.orderStatusEventPublisher = orderStatusEventPublisher;
        this.orderStateMachine = new OrderStateMachine();
    }

    @Transactional
    public OrderStatus updateSubOrderStatus(UUID subOrderId, OrderStatus targetStatus) {
        SubOrder subOrder = subOrderRepository.findByIdForUpdate(subOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Sub-order was not found."));
        orderStateMachine.transition(subOrder, targetStatus);
        OrderStatus status = subOrderRepository.save(subOrder).getStatus();
        orderStatusEventPublisher.enqueue(new OrderStatusChangedEvent(UUID.randomUUID(),
                subOrder.getOrder().getCustomerId(), subOrder.getId(), subOrder.getSellerId(), status));
        return status;
    }
}
