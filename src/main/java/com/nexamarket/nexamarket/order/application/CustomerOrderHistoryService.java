package com.nexamarket.nexamarket.order.application;

import com.nexamarket.nexamarket.order.api.CustomerOrderSummaryResponse;
import com.nexamarket.nexamarket.order.infrastructure.CustomerOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerOrderHistoryService {

    private final CustomerOrderRepository customerOrderRepository;

    @Transactional(readOnly = true)
    public List<CustomerOrderSummaryResponse> listForCustomer(Long customerId) {
        return customerOrderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(CustomerOrderSummaryResponse::from)
                .toList();
    }
}
