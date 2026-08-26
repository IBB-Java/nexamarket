package com.nexamarket.nexamarket.payment.application;

import com.nexamarket.nexamarket.order.domain.CustomerOrder;
import com.nexamarket.nexamarket.order.domain.OrderStateMachine;
import com.nexamarket.nexamarket.order.domain.OrderStatus;
import com.nexamarket.nexamarket.order.domain.SubOrder;
import com.nexamarket.nexamarket.order.infrastructure.CustomerOrderRepository;
import com.nexamarket.nexamarket.payment.domain.PaymentStatus;
import com.nexamarket.nexamarket.payment.domain.PaymentTransaction;
import com.nexamarket.nexamarket.payment.domain.ProcessedPaymentWebhook;
import com.nexamarket.nexamarket.payment.domain.ProviderPaymentStatus;
import com.nexamarket.nexamarket.payment.domain.WalletAccount;
import com.nexamarket.nexamarket.payment.infrastructure.PaymentTransactionRepository;
import com.nexamarket.nexamarket.payment.infrastructure.ProcessedPaymentWebhookRepository;
import com.nexamarket.nexamarket.payment.infrastructure.WalletAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentWebhookService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ProcessedPaymentWebhookRepository processedPaymentWebhookRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final WalletAccountRepository walletAccountRepository;
    private final OrderStateMachine orderStateMachine;

    public PaymentWebhookService(PaymentTransactionRepository paymentTransactionRepository,
                                 ProcessedPaymentWebhookRepository processedPaymentWebhookRepository,
                                 CustomerOrderRepository customerOrderRepository,
                                 WalletAccountRepository walletAccountRepository) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.processedPaymentWebhookRepository = processedPaymentWebhookRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.walletAccountRepository = walletAccountRepository;
        this.orderStateMachine = new OrderStateMachine();
    }

    /** A provider event id is recorded first, so duplicated callbacks are harmless. */
    @Transactional
    public PaymentView handle(PaymentWebhookCommand command) {
        validate(command);
        PaymentTransaction payment = paymentTransactionRepository.findByProviderPaymentIdForUpdate(command.providerPaymentId())
                .orElseThrow(() -> new PaymentException(HttpStatus.NOT_FOUND, "Provider payment was not found."));
        if (processedPaymentWebhookRepository.findByProviderEventId(command.providerEventId()).isPresent()) {
            return PaymentView.from(payment);
        }
        processedPaymentWebhookRepository.save(ProcessedPaymentWebhook.received(
                command.providerEventId(), command.providerPaymentId()));

        if (payment.getStatus() != PaymentStatus.PENDING || command.status() == ProviderPaymentStatus.PENDING) {
            return PaymentView.from(payment);
        }
        if (command.status() == ProviderPaymentStatus.SUCCEEDED) {
            payment.markSucceeded();
            CustomerOrder order = customerOrderRepository.findByIdWithSubOrdersForUpdate(payment.getOrderId())
                    .orElseThrow(() -> new PaymentException(HttpStatus.NOT_FOUND, "Order was not found."));
            markOrderPaid(order);
        } else {
            payment.markFailed(command.failureReason() == null || command.failureReason().isBlank()
                    ? "Provider rejected the payment." : command.failureReason());
            refundWalletComponent(payment);
        }
        return PaymentView.from(paymentTransactionRepository.save(payment));
    }

    private void markOrderPaid(CustomerOrder order) {
        if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            throw new PaymentException(HttpStatus.CONFLICT, "The order is no longer awaiting payment.");
        }
        for (SubOrder subOrder : order.getSubOrders()) {
            orderStateMachine.transition(subOrder, OrderStatus.PAID);
        }
        orderStateMachine.transition(order, OrderStatus.PAID);
    }

    private void refundWalletComponent(PaymentTransaction payment) {
        if (payment.getWalletAmount().signum() == 0) {
            return;
        }
        WalletAccount wallet = walletAccountRepository.findByCustomerIdForUpdate(payment.getCustomerId())
                .orElseThrow(() -> new PaymentException(HttpStatus.CONFLICT, "Wallet account was not found for refund."));
        wallet.credit(payment.getWalletAmount());
        walletAccountRepository.save(wallet);
    }

    private void validate(PaymentWebhookCommand command) {
        if (command == null || command.providerEventId() == null || command.providerEventId().isBlank()
                || command.providerPaymentId() == null || command.status() == null) {
            throw new PaymentException(HttpStatus.BAD_REQUEST, "Provider event id, payment id and status are required.");
        }
    }
}
