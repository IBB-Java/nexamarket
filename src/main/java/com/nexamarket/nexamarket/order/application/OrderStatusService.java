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
        return transitionAndPublish(subOrder, targetStatus);
    }

    /** Only the delivery workflow may advance order state on behalf of a courier. */
    @Transactional
    OrderStatus updateSubOrderStatusFromDelivery(UUID subOrderId, OrderStatus targetStatus, Long courierId) {
        if (!Set.of(OrderStatus.SHIPPED, OrderStatus.DELIVERED).contains(targetStatus)) {
            throw new OrderAccessDeniedException("Teslimat akışı yalnızca SHIPPED veya DELIVERED durumunu üretebilir.");
        }
        SubOrder subOrder = subOrderRepository.findByIdForUpdate(subOrderId)
                .orElseThrow(() -> new OrderNotFoundException("Alt sipariş bulunamadı: " + subOrderId));
        if (!courierId.equals(subOrder.getCourierId())) {
            throw new OrderAccessDeniedException("Bu alt sipariş ilgili kuryeye atanmış değil.");
        }
        if (targetStatus == OrderStatus.SHIPPED && subOrder.getStatus() == OrderStatus.SHIPPED) {
            return OrderStatus.SHIPPED;
        }
        return transitionAndPublish(subOrder, targetStatus);
    }

    private OrderStatus transitionAndPublish(SubOrder subOrder, OrderStatus targetStatus) {
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
        if (actorRole == UserRole.COURIER) {
            throw new OrderAccessDeniedException("Kurye sipariş durumunu yalnızca teslimat adımları üzerinden ilerletebilir.");
        }
        throw new OrderAccessDeniedException("Yalnızca siparişin satıcısı, atanmış kuryesi veya yönetici durum güncelleyebilir.");
    }
}
