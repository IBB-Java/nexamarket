package com.nexamarket.nexamarket.payment.application;

import com.nexamarket.nexamarket.cart.application.CheckoutOrderRequest;
import com.nexamarket.nexamarket.order.domain.CustomerOrder;
import com.nexamarket.nexamarket.order.domain.OrderStatus;
import com.nexamarket.nexamarket.order.infrastructure.CustomerOrderRepository;
import com.nexamarket.nexamarket.payment.domain.PaymentStatus;
import com.nexamarket.nexamarket.payment.domain.PaymentTransaction;
import com.nexamarket.nexamarket.payment.domain.ProcessedPaymentWebhook;
import com.nexamarket.nexamarket.payment.domain.ProviderPaymentStatus;
import com.nexamarket.nexamarket.payment.domain.WalletAccount;
import com.nexamarket.nexamarket.payment.infrastructure.PaymentTransactionRepository;
import com.nexamarket.nexamarket.payment.infrastructure.ProcessedPaymentWebhookRepository;
import com.nexamarket.nexamarket.payment.infrastructure.WalletAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookServiceTest {

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;
    @Mock
    private ProcessedPaymentWebhookRepository processedPaymentWebhookRepository;
    @Mock
    private CustomerOrderRepository customerOrderRepository;
    @Mock
    private WalletAccountRepository walletAccountRepository;

    @Test
    void confirmsParentAndSellerOrdersForASuccessfulWebhook() {
        CustomerOrder order = order();
        PaymentTransaction payment = cardPayment(order, BigDecimal.ZERO, new BigDecimal("30.00"));
        PaymentWebhookService service = service();
        PaymentWebhookCommand command = new PaymentWebhookCommand("event-success", payment.getProviderPaymentId(),
                ProviderPaymentStatus.SUCCEEDED, null);

        when(paymentTransactionRepository.findByProviderPaymentIdForUpdate(payment.getProviderPaymentId()))
                .thenReturn(Optional.of(payment));
        when(processedPaymentWebhookRepository.findByProviderEventId(command.providerEventId())).thenReturn(Optional.empty());
        when(customerOrderRepository.findByIdWithSubOrdersForUpdate(order.getId())).thenReturn(Optional.of(order));
        when(paymentTransactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentView result = service.handle(command);

        assertThat(result.status()).isEqualTo(PaymentStatus.SUCCEEDED.name());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getSubOrders()).allSatisfy(subOrder -> assertThat(subOrder.getStatus()).isEqualTo(OrderStatus.PAID));
        verify(processedPaymentWebhookRepository).save(any(ProcessedPaymentWebhook.class));
    }

    @Test
    void refundsWalletComponentWhenProviderReportsFailure() {
        CustomerOrder order = order();
        PaymentTransaction payment = cardPayment(order, new BigDecimal("7.00"), new BigDecimal("23.00"));
        WalletAccount wallet = WalletAccount.open(order.getCustomerId(), BigDecimal.ZERO);
        PaymentWebhookService service = service();
        PaymentWebhookCommand command = new PaymentWebhookCommand("event-failure", payment.getProviderPaymentId(),
                ProviderPaymentStatus.FAILED, "Card declined");

        when(paymentTransactionRepository.findByProviderPaymentIdForUpdate(payment.getProviderPaymentId()))
                .thenReturn(Optional.of(payment));
        when(processedPaymentWebhookRepository.findByProviderEventId(command.providerEventId())).thenReturn(Optional.empty());
        when(walletAccountRepository.findByCustomerIdForUpdate(order.getCustomerId())).thenReturn(Optional.of(wallet));
        when(paymentTransactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(walletAccountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentView result = service.handle(command);

        assertThat(result.status()).isEqualTo(PaymentStatus.FAILED.name());
        assertThat(wallet.getBalance()).isEqualByComparingTo("7.00");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
    }

    @Test
    void ignoresADuplicateProviderEvent() {
        CustomerOrder order = order();
        PaymentTransaction payment = cardPayment(order, BigDecimal.ZERO, new BigDecimal("30.00"));
        PaymentWebhookService service = service();
        PaymentWebhookCommand command = new PaymentWebhookCommand("event-duplicate", payment.getProviderPaymentId(),
                ProviderPaymentStatus.SUCCEEDED, null);

        when(paymentTransactionRepository.findByProviderPaymentIdForUpdate(payment.getProviderPaymentId()))
                .thenReturn(Optional.of(payment));
        when(processedPaymentWebhookRepository.findByProviderEventId(command.providerEventId()))
                .thenReturn(Optional.of(ProcessedPaymentWebhook.received(command.providerEventId(), payment.getProviderPaymentId())));

        PaymentView result = service.handle(command);

        assertThat(result.status()).isEqualTo(PaymentStatus.PENDING.name());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
        verify(processedPaymentWebhookRepository, never()).save(any());
        verify(customerOrderRepository, never()).findByIdWithSubOrdersForUpdate(any());
    }

    private PaymentWebhookService service() {
        return new PaymentWebhookService(paymentTransactionRepository, processedPaymentWebhookRepository,
                customerOrderRepository, walletAccountRepository);
    }

    private PaymentTransaction cardPayment(CustomerOrder order, BigDecimal walletAmount, BigDecimal cardAmount) {
        PaymentTransaction payment = PaymentTransaction.initiate(order.getId(), order.getCustomerId(), UUID.randomUUID().toString(),
                walletAmount, cardAmount, Instant.parse("2026-08-26T09:00:00Z"));
        payment.assignProviderPayment(UUID.randomUUID());
        return payment;
    }

    private CustomerOrder order() {
        return CustomerOrder.from(new CheckoutOrderRequest(UUID.randomUUID(), UUID.randomUUID(), List.of(
                new CheckoutOrderRequest.SellerOrderRequest(UUID.randomUUID(), List.of(
                        new CheckoutOrderRequest.OrderItemRequest(UUID.randomUUID(), 1, new BigDecimal("30.00"),
                                UUID.randomUUID(), Instant.parse("2026-08-26T12:00:00Z")))))));
    }
}
