package com.nexamarket.nexamarket.order.application;

import com.nexamarket.nexamarket.order.api.CustomerOrderSummaryResponse;
import com.nexamarket.nexamarket.order.infrastructure.CustomerOrderRepository;
import com.nexamarket.nexamarket.order.infrastructure.SubOrderRepository;
import com.nexamarket.nexamarket.order.domain.CustomerOrder;
import com.nexamarket.nexamarket.order.domain.SubOrder;
import com.nexamarket.auth.entity.UserRole;
import com.nexamarket.users.entity.UserProfile;
import com.nexamarket.users.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class CustomerOrderHistoryService {

    private final CustomerOrderRepository customerOrderRepository;
    private final SubOrderRepository subOrderRepository;
    private final UserProfileRepository userProfileRepository;

    @Autowired
    public CustomerOrderHistoryService(CustomerOrderRepository customerOrderRepository,
                                       SubOrderRepository subOrderRepository,
                                       UserProfileRepository userProfileRepository) {
        this.customerOrderRepository = customerOrderRepository;
        this.subOrderRepository = subOrderRepository;
        this.userProfileRepository = userProfileRepository;
    }

    CustomerOrderHistoryService(CustomerOrderRepository customerOrderRepository) {
        this(customerOrderRepository, null, null);
    }

    CustomerOrderHistoryService(CustomerOrderRepository customerOrderRepository,
                                SubOrderRepository subOrderRepository) {
        this(customerOrderRepository, subOrderRepository, null);
    }

    @Transactional(readOnly = true)
    public List<CustomerOrderSummaryResponse> listForCustomer(Long customerId) {
        return customerOrderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(this::summaryFor)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CustomerOrderSummaryResponse> listVisibleFor(Long userId, UserRole role) {
        return switch (role) {
            case CUSTOMER -> listForCustomer(userId);
            case ADMIN -> customerOrderRepository.findAllWithSubOrdersOrderByCreatedAtDesc().stream()
                    .map(this::summaryFor)
                    .toList();
            case SELLER -> summariesForSubOrders(subOrderRepository.findBySellerIdWithOrder(userId));
            case COURIER -> summariesForSubOrders(subOrderRepository.findByCourierIdWithOrder(userId));
        };
    }

    private List<CustomerOrderSummaryResponse> summariesForSubOrders(List<SubOrder> subOrders) {
        Map<CustomerOrder, List<SubOrder>> grouped = new LinkedHashMap<>();
        subOrders.forEach(subOrder -> grouped.computeIfAbsent(subOrder.getOrder(), ignored -> new java.util.ArrayList<>())
                .add(subOrder));
        return grouped.entrySet().stream()
                .map(entry -> summaryFor(entry.getKey(), entry.getValue()))
                .toList();
    }

    private CustomerOrderSummaryResponse summaryFor(CustomerOrder order) {
        return summaryFor(order, order.getSubOrders());
    }

    private CustomerOrderSummaryResponse summaryFor(CustomerOrder order, List<SubOrder> visibleSubOrders) {
        return CustomerOrderSummaryResponse.from(order, visibleSubOrders, customerNameFor(order));
    }

    private String customerNameFor(CustomerOrder order) {
        if (userProfileRepository != null) {
            String profileName = userProfileRepository.findByUserId(order.getCustomerId())
                    .map(this::fullName)
                    .orElse(null);
            if (profileName != null) {
                return profileName;
            }
        }
        String email = order.getCustomerEmail();
        if (email == null || email.isBlank()) {
            return "Silinmiş kullanıcı";
        }
        int atIndex = email.indexOf('@');
        return atIndex > 0 ? email.substring(0, atIndex) : email;
    }

    private String fullName(UserProfile profile) {
        String firstName = profile.getFirstName() == null ? "" : profile.getFirstName().trim();
        String lastName = profile.getLastName() == null ? "" : profile.getLastName().trim();
        String name = (firstName + " " + lastName).trim();
        return name.isEmpty() ? null : name;
    }
}
