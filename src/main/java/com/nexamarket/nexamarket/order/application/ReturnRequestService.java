package com.nexamarket.nexamarket.order.application;

import com.nexamarket.nexamarket.order.domain.OrderStateMachine;
import com.nexamarket.nexamarket.order.domain.OrderStatus;
import com.nexamarket.nexamarket.order.domain.ReturnRequest;
import com.nexamarket.nexamarket.order.domain.ReturnRequestStatus;
import com.nexamarket.nexamarket.order.domain.SubOrder;
import com.nexamarket.nexamarket.order.infrastructure.ReturnRequestRepository;
import com.nexamarket.nexamarket.order.infrastructure.SubOrderRepository;
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

    @Autowired
    public ReturnRequestService(SubOrderRepository subOrderRepository, ReturnRequestRepository returnRequestRepository) {
        this(subOrderRepository, returnRequestRepository, Clock.systemUTC());
    }

    ReturnRequestService(SubOrderRepository subOrderRepository, ReturnRequestRepository returnRequestRepository, Clock clock) {
        this.subOrderRepository = subOrderRepository;
        this.returnRequestRepository = returnRequestRepository;
        this.orderStateMachine = new OrderStateMachine();
        this.clock = clock;
    }

    @Transactional
    public ReturnRequest create(CreateReturnRequestCommand command) {
        Objects.requireNonNull(command, "Create-return command is required.");
        SubOrder subOrder = subOrderRepository.findByIdForUpdate(command.subOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Sub-order was not found."));
        orderStateMachine.transition(subOrder, OrderStatus.RETURN_REQUESTED);
        return returnRequestRepository.save(new ReturnRequest(subOrder, command.reason()));
    }

    @Transactional
    public ReturnRequest resolve(ResolveReturnRequestCommand command) {
        Objects.requireNonNull(command, "Resolve-return command is required.");
        ReturnRequest returnRequest = returnRequestRepository.findByIdForUpdate(command.returnRequestId())
                .orElseThrow(() -> new IllegalArgumentException("Return request was not found."));

        OrderStatus targetOrderStatus = switch (command.status()) {
            case APPROVED -> OrderStatus.RETURN_APPROVED;
            case REJECTED -> OrderStatus.RETURN_REJECTED;
            case REQUESTED -> throw new IllegalArgumentException("Return resolution must be approved or rejected.");
        };
        orderStateMachine.transition(returnRequest.getSubOrder(), targetOrderStatus);
        returnRequest.resolve(command.status(), command.resolverId(), Instant.now(clock));
        return returnRequestRepository.save(returnRequest);
    }
}
