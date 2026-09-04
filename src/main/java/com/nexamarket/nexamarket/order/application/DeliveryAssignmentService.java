package com.nexamarket.nexamarket.order.application;

import com.nexamarket.nexamarket.order.api.AdminDeliveryAssignmentResponse;
import com.nexamarket.nexamarket.order.api.CourierDeliveryResponse;
import com.nexamarket.nexamarket.order.domain.DeliveryAssignment;
import com.nexamarket.nexamarket.order.domain.DeliveryAssignmentStateMachine;
import com.nexamarket.nexamarket.order.domain.DeliveryAssignmentStatus;
import com.nexamarket.nexamarket.order.domain.DeliveryFailureReasonCode;
import com.nexamarket.nexamarket.order.domain.OrderStatus;
import com.nexamarket.nexamarket.order.domain.SubOrder;
import com.nexamarket.nexamarket.order.infrastructure.DeliveryAssignmentRepository;
import com.nexamarket.nexamarket.order.infrastructure.SubOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryAssignmentService {

    private static final EnumSet<OrderStatus> ASSIGNABLE_ORDER_STATUSES =
            EnumSet.of(OrderStatus.PAID, OrderStatus.PROCESSING, OrderStatus.SHIPPED);

    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final SubOrderRepository subOrderRepository;
    private final CourierDirectoryGateway courierDirectoryGateway;
    private final OrderStatusService orderStatusService;
    private final DeliveryStatusEventPublisher deliveryStatusEventPublisher;
    private final Clock clock = Clock.systemUTC();
    private final DeliveryAssignmentStateMachine stateMachine = new DeliveryAssignmentStateMachine();

    @Transactional(readOnly = true)
    public List<CourierDeliveryResponse> listForCourier(Long courierId) {
        return deliveryAssignmentRepository.findByCourierIdWithOrder(courierId).stream()
                .map(CourierDeliveryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminDeliveryAssignmentResponse> listAll() {
        return deliveryAssignmentRepository.findAllWithOrder().stream()
                .map(AdminDeliveryAssignmentResponse::from)
                .toList();
    }

    @Transactional
    public AdminDeliveryAssignmentResponse assign(UUID subOrderId, Long courierId) {
        if (!courierDirectoryGateway.isActiveCourier(courierId)) {
            throw new InvalidCourierAssignmentException("Yalnızca aktif bir COURIER kullanıcısı atanabilir.");
        }
        SubOrder subOrder = subOrderRepository.findByIdForUpdate(subOrderId)
                .orElseThrow(() -> new OrderNotFoundException("Alt sipariş bulunamadı: " + subOrderId));
        if (!ASSIGNABLE_ORDER_STATUSES.contains(subOrder.getStatus())) {
            throw new InvalidCourierAssignmentException(
                    "Kurye yalnızca PAID, PROCESSING veya SHIPPED durumundaki alt siparişe atanabilir.");
        }
        deliveryAssignmentRepository.findActiveBySubOrderIdForUpdate(subOrderId).ifPresent(existing -> {
            throw new InvalidCourierAssignmentException("Bu alt siparişin zaten aktif bir kurye ataması var.");
        });

        DeliveryAssignment assignment = DeliveryAssignment.assign(subOrder, courierId, Instant.now(clock));
        subOrder.assignCourier(courierId);
        subOrderRepository.save(subOrder);
        DeliveryAssignment saved = deliveryAssignmentRepository.saveAndFlush(assignment);
        publish(saved, courierId);
        return AdminDeliveryAssignmentResponse.from(saved);
    }

    @Transactional
    public CourierDeliveryResponse accept(UUID assignmentId, Long courierId) {
        DeliveryAssignment assignment = ownedForUpdate(assignmentId, courierId);
        transition(assignment, DeliveryAssignmentStatus.ACCEPTED, null, null);
        return saveAndNotifyCustomer(assignment);
    }

    @Transactional
    public CourierDeliveryResponse reject(UUID assignmentId, Long courierId, String reason) {
        DeliveryAssignment assignment = ownedForUpdate(assignmentId, courierId);
        transition(assignment, DeliveryAssignmentStatus.REJECTED, null, reason.trim());
        assignment.getSubOrder().clearCourier(courierId);
        subOrderRepository.save(assignment.getSubOrder());
        return saveAndNotifyCustomer(assignment);
    }

    @Transactional
    public CourierDeliveryResponse pickup(UUID assignmentId, Long courierId) {
        DeliveryAssignment assignment = ownedForUpdate(assignmentId, courierId);
        transition(assignment, DeliveryAssignmentStatus.PICKED_UP, null, null);
        orderStatusService.updateSubOrderStatusFromDelivery(
                assignment.getSubOrder().getId(), OrderStatus.SHIPPED, courierId);
        return saveAndNotifyCustomer(assignment);
    }

    @Transactional
    public CourierDeliveryResponse start(UUID assignmentId, Long courierId) {
        DeliveryAssignment assignment = ownedForUpdate(assignmentId, courierId);
        transition(assignment, DeliveryAssignmentStatus.IN_TRANSIT, null, null);
        return saveAndNotifyCustomer(assignment);
    }

    @Transactional
    public CourierDeliveryResponse deliver(UUID assignmentId, Long courierId) {
        DeliveryAssignment assignment = ownedForUpdate(assignmentId, courierId);
        transition(assignment, DeliveryAssignmentStatus.DELIVERED, null, null);
        orderStatusService.updateSubOrderStatusFromDelivery(
                assignment.getSubOrder().getId(), OrderStatus.DELIVERED, courierId);
        return saveAndNotifyCustomer(assignment);
    }

    @Transactional
    public CourierDeliveryResponse fail(UUID assignmentId, Long courierId,
                                        DeliveryFailureReasonCode reasonCode, String description) {
        DeliveryAssignment assignment = ownedForUpdate(assignmentId, courierId);
        transition(assignment, DeliveryAssignmentStatus.DELIVERY_FAILED, reasonCode, description.trim());
        assignment.getSubOrder().clearCourier(courierId);
        subOrderRepository.save(assignment.getSubOrder());
        return saveAndNotifyCustomer(assignment);
    }

    private DeliveryAssignment ownedForUpdate(UUID assignmentId, Long courierId) {
        DeliveryAssignment assignment = deliveryAssignmentRepository.findByIdForUpdate(assignmentId)
                .orElseThrow(() -> new DeliveryAssignmentNotFoundException(assignmentId));
        if (!assignment.getCourierId().equals(courierId)) {
            throw new OrderAccessDeniedException("Bu teslimat ataması başka bir kuryeye ait.");
        }
        return assignment;
    }

    private void transition(DeliveryAssignment assignment, DeliveryAssignmentStatus target,
                            DeliveryFailureReasonCode reasonCode, String detail) {
        stateMachine.transition(assignment, target, Instant.now(clock), reasonCode, detail);
    }

    private CourierDeliveryResponse saveAndNotifyCustomer(DeliveryAssignment assignment) {
        DeliveryAssignment saved = deliveryAssignmentRepository.save(assignment);
        publish(saved, saved.getSubOrder().getOrder().getCustomerId());
        return CourierDeliveryResponse.from(saved);
    }

    private void publish(DeliveryAssignment assignment, Long recipientId) {
        deliveryStatusEventPublisher.enqueue(new DeliveryStatusChangedEvent(
                UUID.randomUUID(), recipientId, assignment.getId(), assignment.getSubOrder().getId(),
                assignment.getStatus()));
    }
}
