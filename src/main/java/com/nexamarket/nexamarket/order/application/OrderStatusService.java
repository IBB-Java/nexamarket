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

    public OrderStatusService(SubOrderRepository subOrderRepository) {
        this.subOrderRepository = subOrderRepository;
        this.orderStateMachine = new OrderStateMachine();
    }

    @Transactional
    public OrderStatus updateSubOrderStatus(UUID subOrderId, OrderStatus targetStatus) {
        SubOrder subOrder = subOrderRepository.findByIdForUpdate(subOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Sub-order was not found."));
        orderStateMachine.transition(subOrder, targetStatus);
        return subOrderRepository.save(subOrder).getStatus();
    }
}
