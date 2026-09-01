package com.nexamarket.nexamarket.payment.application;

import com.nexamarket.nexamarket.order.domain.CustomerOrder;
import com.nexamarket.nexamarket.order.domain.OrderStateMachine;
import com.nexamarket.nexamarket.order.domain.OrderStatus;
import com.nexamarket.nexamarket.order.domain.SubOrder;
import com.nexamarket.nexamarket.order.infrastructure.CustomerOrderRepository;
import com.nexamarket.nexamarket.order.application.OrderStatusChangedEvent;
import com.nexamarket.nexamarket.order.application.OrderStatusEventPublisher;
import com.nexamarket.nexamarket.payment.domain.PaymentStatus;
import com.nexamarket.nexamarket.payment.domain.PaymentTransaction;
import com.nexamarket.nexamarket.payment.domain.WalletAccount;
import com.nexamarket.nexamarket.payment.infrastructure.PaymentTransactionRepository;
import com.nexamarket.nexamarket.payment.infrastructure.WalletAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Service
public class PaymentApplicationService {

    private final CustomerOrderRepository customerOrderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final WalletAccountRepository walletAccountRepository;
    private final PaymentProviderGateway paymentProviderGateway;
    private final OrderStateMachine orderStateMachine;
    private final Clock clock;
    private final Duration pollingInterval;
    private final StockReservationCommitGateway stockReservationCommitGateway;
    private final OrderStatusEventPublisher orderStatusEventPublisher;

    @Autowired
    public PaymentApplicationService(CustomerOrderRepository customerOrderRepository,
                                     PaymentTransactionRepository paymentTransactionRepository,
                                     WalletAccountRepository walletAccountRepository,
                                     PaymentProviderGateway paymentProviderGateway,
                                     StockReservationCommitGateway stockReservationCommitGateway,
                                     OrderStatusEventPublisher orderStatusEventPublisher,
                                     @Value("${payment.polling.interval}") Duration pollingInterval) {
        this(customerOrderRepository, paymentTransactionRepository, walletAccountRepository,
                paymentProviderGateway, stockReservationCommitGateway, orderStatusEventPublisher, pollingInterval, Clock.systemUTC());
    }

    PaymentApplicationService(CustomerOrderRepository customerOrderRepository,
                              PaymentTransactionRepository paymentTransactionRepository,
                              WalletAccountRepository walletAccountRepository,
                              PaymentProviderGateway paymentProviderGateway,
                              Duration pollingInterval, Clock clock) {
        this(customerOrderRepository, paymentTransactionRepository, walletAccountRepository,
                paymentProviderGateway, null, null, pollingInterval, clock);
    }

    PaymentApplicationService(CustomerOrderRepository customerOrderRepository,
                              PaymentTransactionRepository paymentTransactionRepository,
                              WalletAccountRepository walletAccountRepository,
                              PaymentProviderGateway paymentProviderGateway,
                              StockReservationCommitGateway stockReservationCommitGateway,
                              OrderStatusEventPublisher orderStatusEventPublisher,
                              Duration pollingInterval, Clock clock) {
        this.customerOrderRepository = customerOrderRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.walletAccountRepository = walletAccountRepository;
        this.paymentProviderGateway = paymentProviderGateway;
        this.orderStateMachine = new OrderStateMachine();
        this.pollingInterval = pollingInterval;
        this.clock = clock;
        this.stockReservationCommitGateway = stockReservationCommitGateway;
        this.orderStatusEventPublisher = orderStatusEventPublisher;
    }

    /**
     * The client-provided idempotency key makes a network retry return the
     * original payment instead of charging card or wallet a second time.
     */
    @Transactional
    public PaymentView initiate(InitiatePaymentCommand command) {
        validate(command);

        PaymentTransaction replay = paymentTransactionRepository.findByIdempotencyKey(command.idempotencyKey())
                .orElse(null);
        if (replay != null) {
            validateReplay(command, replay);
            return PaymentView.from(replay);
        }

        CustomerOrder order = customerOrderRepository.findByIdWithSubOrdersForUpdate(command.orderId())
                .orElseThrow(() -> new PaymentException(HttpStatus.NOT_FOUND, "Order was not found."));
        if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            throw new PaymentException(HttpStatus.CONFLICT, "Only payment-pending orders can be paid.");
        }
        if (command.customerId() != null && !order.getCustomerId().equals(command.customerId())) {
            throw new PaymentException(HttpStatus.FORBIDDEN, "A customer can only pay for their own order.");
        }
        if (paymentTransactionRepository.findFirstByOrderIdOrderByCreatedAtDesc(order.getId())
                .filter(payment -> payment.getStatus() != PaymentStatus.FAILED)
                .isPresent()) {
            throw new PaymentException(HttpStatus.CONFLICT, "This order already has an active or successful payment.");
        }
        if (command.walletAmount().add(command.cardAmount()).compareTo(order.getTotalAmount()) != 0) {
            throw new PaymentException(HttpStatus.BAD_REQUEST, "Wallet and card amounts must equal the order total.");
        }

        debitWalletWhenNeeded(order, command.walletAmount());
        Instant nextPollAt = command.cardAmount().signum() > 0 ? Instant.now(clock).plus(pollingInterval) : null;
        PaymentTransaction payment = PaymentTransaction.initiate(order.getId(), order.getCustomerId(),
                command.idempotencyKey(), command.walletAmount(), command.cardAmount(), nextPollAt);
        paymentTransactionRepository.save(payment);

        if (command.cardAmount().signum() > 0) {
            PaymentProviderGateway.ProviderPayment providerPayment = paymentProviderGateway.createCardPayment(
                    payment.getId(), command.cardAmount());
            payment.assignProviderPayment(providerPayment.providerPaymentId());
        } else {
            payment.markSucceeded();
            markOrderPaid(order);
        }
        return PaymentView.from(paymentTransactionRepository.save(payment));
    }

    private void debitWalletWhenNeeded(CustomerOrder order, BigDecimal walletAmount) {
        if (walletAmount.signum() == 0) {
            return;
        }
        WalletAccount wallet = walletAccountRepository.findByCustomerIdForUpdate(order.getCustomerId())
                .orElseThrow(() -> new PaymentException(HttpStatus.CONFLICT, "Wallet account was not found."));
        try {
            wallet.debit(walletAmount);
        } catch (IllegalStateException exception) {
            throw new PaymentException(HttpStatus.CONFLICT, exception.getMessage());
        }
        walletAccountRepository.save(wallet);
    }

    private void markOrderPaid(CustomerOrder order) {
        if (stockReservationCommitGateway != null) {
            for (SubOrder subOrder : order.getSubOrders()) {
                subOrder.getItems().forEach(item -> stockReservationCommitGateway.confirm(item.getStockReservationCode()));
            }
        }
        for (SubOrder subOrder : order.getSubOrders()) {
            orderStateMachine.transition(subOrder, OrderStatus.PAID);
            publishStatus(order, subOrder);
        }
        orderStateMachine.transition(order, OrderStatus.PAID);
    }

    private void publishStatus(CustomerOrder order, SubOrder subOrder) {
        if (orderStatusEventPublisher != null) {
            orderStatusEventPublisher.enqueue(new OrderStatusChangedEvent(java.util.UUID.randomUUID(), order.getCustomerId(),
                    subOrder.getId(), subOrder.getSellerId(), subOrder.getStatus()));
        }
    }

    private void validate(InitiatePaymentCommand command) {
        if (command == null || command.orderId() == null || command.idempotencyKey() == null
                || command.idempotencyKey().isBlank()) {
            throw new PaymentException(HttpStatus.BAD_REQUEST, "Order id and idempotency key are required.");
        }
        if (command.walletAmount() == null || command.cardAmount() == null
                || command.walletAmount().signum() < 0 || command.cardAmount().signum() < 0
                || (command.walletAmount().signum() == 0 && command.cardAmount().signum() == 0)) {
            throw new PaymentException(HttpStatus.BAD_REQUEST, "At least one non-negative payment amount is required.");
        }
    }

    private void validateReplay(InitiatePaymentCommand command, PaymentTransaction replay) {
        if (!Objects.equals(command.orderId(), replay.getOrderId())
                || command.walletAmount().compareTo(replay.getWalletAmount()) != 0
                || command.cardAmount().compareTo(replay.getCardAmount()) != 0) {
            throw new PaymentException(HttpStatus.CONFLICT, "Idempotency key was already used for a different payment.");
        }
    }
}
