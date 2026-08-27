package com.nexamarket.nexamarket.order.application;

import com.nexamarket.nexamarket.order.domain.OrderStateMachine;
import com.nexamarket.nexamarket.order.domain.OrderStatus;
import com.nexamarket.nexamarket.order.domain.ReturnRequest;
import com.nexamarket.nexamarket.order.domain.ReturnRequestStatus;
import com.nexamarket.nexamarket.order.domain.SubOrder;
import com.nexamarket.nexamarket.order.infrastructure.ReturnRequestRepository;
import com.nexamarket.nexamarket.order.infrastructure.SubOrderRepository;
import com.nexamarket.loyalty.application.LoyaltyService;
import com.nexamarket.auth.entity.UserRole;
import com.nexamarket.auth.security.AuthPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

@Service
public class ReturnRequestService {

    private final SubOrderRepository subOrderRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final OrderStateMachine orderStateMachine;
    private final Clock clock;
    private final LoyaltyService loyaltyService;
    private final OrderStatusEventPublisher orderStatusEventPublisher;

    @Autowired
    public ReturnRequestService(SubOrderRepository subOrderRepository, ReturnRequestRepository returnRequestRepository,
                                LoyaltyService loyaltyService, OrderStatusEventPublisher orderStatusEventPublisher) {
        this(subOrderRepository, returnRequestRepository, Clock.systemUTC(), loyaltyService, orderStatusEventPublisher);
    }

    ReturnRequestService(SubOrderRepository subOrderRepository, ReturnRequestRepository returnRequestRepository, Clock clock) {
        this(subOrderRepository, returnRequestRepository, clock, null, null);
    }

    ReturnRequestService(SubOrderRepository subOrderRepository, ReturnRequestRepository returnRequestRepository, Clock clock,
                         LoyaltyService loyaltyService) {
        this(subOrderRepository, returnRequestRepository, clock, loyaltyService, null);
    }

    ReturnRequestService(SubOrderRepository subOrderRepository, ReturnRequestRepository returnRequestRepository, Clock clock,
                         LoyaltyService loyaltyService, OrderStatusEventPublisher orderStatusEventPublisher) {
        this.subOrderRepository = subOrderRepository;
        this.returnRequestRepository = returnRequestRepository;
        this.orderStateMachine = new OrderStateMachine();
        this.clock = clock;
        this.loyaltyService = loyaltyService;
        this.orderStatusEventPublisher = orderStatusEventPublisher;
    }

    @Transactional
    public ReturnRequest create(CreateReturnRequestCommand command) {
        return create(command, null);
    }

    @Transactional
    public ReturnRequest create(CreateReturnRequestCommand command, AuthPrincipal principal) {
        Objects.requireNonNull(command, "Create-return command is required.");
        SubOrder subOrder = subOrderRepository.findByIdForUpdate(command.subOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Sub-order was not found."));
        if (principal != null && principal.role() != UserRole.ADMIN
                && !subOrder.getOrder().getCustomerId().equals(principal.userId())) {
            throw new OrderAccessDeniedException("Yalnızca siparişi veren müşteri iade talebi açabilir.");
        }
        orderStateMachine.transition(subOrder, OrderStatus.RETURN_REQUESTED);
        ReturnRequest created = returnRequestRepository.save(new ReturnRequest(subOrder, command.reason()));
        publishStatus(subOrder);
        return created;
    }

    @Transactional
    public ReturnRequest resolve(ResolveReturnRequestCommand command) {
        return resolve(command, null);
    }

    @Transactional
    public ReturnRequest resolve(ResolveReturnRequestCommand command, AuthPrincipal principal) {
        Objects.requireNonNull(command, "Resolve-return command is required.");
        ReturnRequest returnRequest = returnRequestRepository.findByIdForUpdate(command.returnRequestId())
                .orElseThrow(() -> new IllegalArgumentException("Return request was not found."));
        if (principal != null && principal.role() != UserRole.ADMIN
                && (principal.role() != UserRole.SELLER
                || !returnRequest.getSubOrder().getSellerId().equals(principal.userId()))) {
            throw new OrderAccessDeniedException("İade talebini yalnızca ilgili satıcı veya yönetici çözebilir.");
        }

        OrderStatus targetOrderStatus = switch (command.status()) {
            case APPROVED -> OrderStatus.RETURN_APPROVED;
            case REJECTED -> OrderStatus.RETURN_REJECTED;
            case REQUESTED -> throw new IllegalArgumentException("Return resolution must be approved or rejected.");
        };
        orderStateMachine.transition(returnRequest.getSubOrder(), targetOrderStatus);
        returnRequest.resolve(command.status(), command.resolverId(), Instant.now(clock));
        ReturnRequest saved = returnRequestRepository.save(returnRequest);
        if (targetOrderStatus == OrderStatus.RETURN_APPROVED && loyaltyService != null) {
            loyaltyService.reverseForApprovedReturn(saved.getSubOrder());
        }
        publishStatus(saved.getSubOrder());
        return saved;
    }

    private void publishStatus(SubOrder subOrder) {
        if (orderStatusEventPublisher != null) {
            orderStatusEventPublisher.enqueue(new OrderStatusChangedEvent(java.util.UUID.randomUUID(),
                    subOrder.getOrder().getCustomerId(), subOrder.getId(), subOrder.getSellerId(), subOrder.getStatus()));
        }
    }
}
