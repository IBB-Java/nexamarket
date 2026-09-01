package com.nexamarket.nexamarket.order.application;

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
    private final CourierDirectoryGateway courierDirectoryGateway;

    @Transactional
    public CourierOrderResponse assign(UUID subOrderId, Long courierId) {
        if (!courierDirectoryGateway.isActiveCourier(courierId)) {
            throw new OrderAccessDeniedException("Yalnızca aktif bir COURIER hesabı atanabilir.");
        }
        SubOrder subOrder = subOrderRepository.findByIdForUpdate(subOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Sub-order was not found."));
        subOrder.assignCourier(courierId);
        return CourierOrderResponse.from(subOrderRepository.save(subOrder));
    }

    @Transactional(readOnly = true)
    public List<CourierOrderResponse> listAssigned(Long courierId) {
        return subOrderRepository.findByCourierIdWithOrder(courierId).stream()
                .map(CourierOrderResponse::from)
                .toList();
    }
}
