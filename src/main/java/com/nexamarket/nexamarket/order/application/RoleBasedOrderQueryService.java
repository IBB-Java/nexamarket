package com.nexamarket.nexamarket.order.application;

import com.nexamarket.auth.entity.UserRole;
import com.nexamarket.auth.security.AuthPrincipal;
import com.nexamarket.nexamarket.order.api.RoleOrderResponse;
import com.nexamarket.nexamarket.order.domain.SubOrder;
import com.nexamarket.nexamarket.order.infrastructure.SubOrderRepository;
import com.nexamarket.nexamarket.order.infrastructure.DeliveryAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoleBasedOrderQueryService {

    private final SubOrderRepository subOrderRepository;
    private final DeliveryAssignmentRepository deliveryAssignmentRepository;

    @Transactional(readOnly = true)
    public List<RoleOrderResponse> listVisibleOrders(AuthPrincipal principal) {
        List<SubOrder> orders = switch (principal.role()) {
            case CUSTOMER -> subOrderRepository.findByCustomerIdWithOrder(principal.userId());
            case SELLER -> subOrderRepository.findBySellerIdWithOrder(principal.userId());
            case COURIER -> deliveryAssignmentRepository.findByCourierIdWithOrder(principal.userId()).stream()
                    .map(assignment -> assignment.getSubOrder())
                    .distinct()
                    .toList();
            case ADMIN -> subOrderRepository.findAllWithOrderOrderByCreatedAtDesc();
        };
        return toResponses(orders, canSeeCustomerId(principal.role()));
    }

    @Transactional(readOnly = true)
    public List<RoleOrderResponse> getVisibleOrder(UUID orderId, AuthPrincipal principal) {
        List<SubOrder> visible = subOrderRepository.findByOrderIdWithOrder(orderId).stream()
                .filter(subOrder -> canView(subOrder, principal))
                .toList();
        if (visible.isEmpty()) {
            throw new OrderAccessDeniedException("Bu siparişi görüntüleme yetkiniz yok.");
        }
        return toResponses(visible, canSeeCustomerId(principal.role()));
    }

    private List<RoleOrderResponse> toResponses(List<SubOrder> subOrders, boolean includeCustomerId) {
        return subOrders.stream()
                .map(subOrder -> RoleOrderResponse.from(subOrder, includeCustomerId))
                .toList();
    }

    private boolean canView(SubOrder subOrder, AuthPrincipal principal) {
        return switch (principal.role()) {
            case CUSTOMER -> subOrder.getOrder().getCustomerId().equals(principal.userId());
            case SELLER -> subOrder.getSellerId().equals(principal.userId());
            case COURIER -> deliveryAssignmentRepository.existsBySubOrder_IdAndCourierId(
                    subOrder.getId(), principal.userId());
            case ADMIN -> true;
        };
    }

    private boolean canSeeCustomerId(UserRole role) {
        return role == UserRole.CUSTOMER || role == UserRole.ADMIN;
    }
}
