package com.nexamarket.nexamarket.order.application;

import com.nexamarket.nexamarket.order.api.CourierOrderResponse;
import com.nexamarket.nexamarket.order.infrastructure.SubOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourierAssignmentService {

    private final SubOrderRepository subOrderRepository;

    @Transactional(readOnly = true)
    public List<CourierOrderResponse> listAssigned(Long courierId) {
        return subOrderRepository.findByCourierIdWithOrder(courierId).stream()
                .map(CourierOrderResponse::from)
                .toList();
    }
}
