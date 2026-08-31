package com.nexamarket.nexamarket.order.application;

import com.nexamarket.nexamarket.order.domain.OrderStateMachine;
import com.nexamarket.nexamarket.order.domain.OrderStatus;
import com.nexamarket.nexamarket.order.domain.SubOrder;
import com.nexamarket.nexamarket.order.infrastructure.SubOrderRepository;
import com.nexamarket.auth.entity.UserRole;
import com.nexamarket.loyalty.application.LoyaltyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.Set;

@Service
public class OrderStatusService {

    private static final Set<OrderStatus> COURIER_STATUSES = Set.of(OrderStatus.SHIPPED, OrderStatus.DELIVERED);

    private final SubOrderRepository subOrderRepository;
    private final OrderStateMachine orderStateMachine;
    private final OrderStatusEventPublisher orderStatusEventPublisher;
    private final LoyaltyService loyaltyService;

    @Autowired
    public OrderStatusService(SubOrderRepository subOrderRepository, OrderStatusEventPublisher orderStatusEventPublisher,
                              LoyaltyService loyaltyService) {
        this.subOrderRepository = subOrderRepository;
        this.orderStatusEventPublisher = orderStatusEventPublisher;
        this.loyaltyService = loyaltyService;
        this.orderStateMachine = new OrderStateMachine();
    }

    OrderStatusService(SubOrderRepository subOrderRepository, OrderStatusEventPublisher orderStatusEventPublisher) {
        this(subOrderRepository, orderStatusEventPublisher, null);
    }

    @Transactional
    public OrderStatus updateSubOrderStatus(UUID subOrderId, OrderStatus targetStatus) {
        return updateSubOrderStatus(subOrderId, targetStatus, null, null);
    }

    @Transactional
    public OrderStatus updateSubOrderStatus(UUID subOrderId, OrderStatus targetStatus, Long actorId, UserRole actorRole) {
        SubOrder subOrder = subOrderRepository.findByIdForUpdate(subOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Sub-order was not found."));
        validateActor(subOrder, targetStatus, actorId, actorRole);
        orderStateMachine.transition(subOrder, targetStatus);
        OrderStatus status = subOrderRepository.save(subOrder).getStatus();
        if (status == OrderStatus.DELIVERED && loyaltyService != null) {
            loyaltyService.awardForDelivery(subOrder);
        }
        orderStatusEventPublisher.enqueue(new OrderStatusChangedEvent(UUID.randomUUID(),
                subOrder.getOrder().getCustomerId(), subOrder.getId(), subOrder.getSellerId(), status));
        return status;
    }

    private void validateActor(SubOrder subOrder, OrderStatus targetStatus, Long actorId, UserRole actorRole) {
        // Null actor is reserved for trusted internal SYSTEM processes such as payment and timeout consumers.
        if (actorId == null) {
            return;
        }
        if (actorRole == UserRole.ADMIN) {
            return;
        }
        if (actorRole == UserRole.SELLER && subOrder.getSellerId().equals(actorId)) {
            return;
        }
        if (actorRole == UserRole.COURIER
                && actorId.equals(subOrder.getCourierId())
                && COURIER_STATUSES.contains(targetStatus)) {
            return;
        }
        if (actorRole == UserRole.COURIER) {
            throw new OrderAccessDeniedException(
                    "Kurye yalnızca kendisine atanan siparişi SHIPPED veya DELIVERED durumuna taşıyabilir.");
        }
        throw new OrderAccessDeniedException("Yalnızca siparişin satıcısı, atanmış kuryesi veya yönetici durum güncelleyebilir.");
    }
}
