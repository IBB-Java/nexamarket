package com.nexamarket.nexamarket.order.application;

import com.nexamarket.auth.entity.UserAccount;
import com.nexamarket.auth.entity.UserRole;
import com.nexamarket.auth.entity.UserStatus;
import com.nexamarket.auth.repository.UserAccountRepository;
import com.nexamarket.nexamarket.order.api.CourierOrderResponse;
import com.nexamarket.nexamarket.order.domain.SubOrder;
import com.nexamarket.nexamarket.order.infrastructure.SubOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourierAssignmentService {

    private final SubOrderRepository subOrderRepository;
    private final UserAccountRepository userAccountRepository;

    @Transactional
    public CourierOrderResponse assign(UUID subOrderId, Long courierId) {
        UserAccount courier = userAccountRepository.findById(courierId)
                .filter(user -> user.getRole() == UserRole.COURIER && user.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new OrderAccessDeniedException("Yalnızca aktif bir COURIER hesabı atanabilir."));
        SubOrder subOrder = subOrderRepository.findByIdForUpdate(subOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Sub-order was not found."));
        subOrder.assignCourier(courier.getId());
        return CourierOrderResponse.from(subOrderRepository.save(subOrder));
    }

    @Transactional(readOnly = true)
    public List<CourierOrderResponse> listAssigned(Long courierId) {
        return subOrderRepository.findByCourierIdWithOrder(courierId).stream()
                .map(CourierOrderResponse::from)
                .toList();
    }
}
