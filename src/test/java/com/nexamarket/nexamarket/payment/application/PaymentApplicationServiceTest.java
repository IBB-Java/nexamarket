package com.nexamarket.nexamarket.payment.application;

import com.nexamarket.nexamarket.cart.application.CheckoutOrderRequest;
import com.nexamarket.nexamarket.order.domain.CustomerOrder;
import com.nexamarket.nexamarket.order.domain.OrderStatus;
import com.nexamarket.nexamarket.order.infrastructure.CustomerOrderRepository;
import com.nexamarket.nexamarket.payment.domain.PaymentStatus;
import com.nexamarket.nexamarket.payment.domain.WalletAccount;
import com.nexamarket.nexamarket.payment.infrastructure.PaymentTransactionRepository;
import com.nexamarket.nexamarket.payment.infrastructure.WalletAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentApplicationServiceTest {

    @Mock
    private CustomerOrderRepository customerOrderRepository;
    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;
    @Mock
    private WalletAccountRepository walletAccountRepository;
    @Mock
    private PaymentProviderGateway paymentProviderGateway;

    @Test
    void startsAnIdempotentCardAndWalletPayment() {
        CustomerOrder order = order("30.00");
        WalletAccount wallet = WalletAccount.open(order.getCustomerId(), new BigDecimal("12.00"));
        InitiatePaymentCommand command = new InitiatePaymentCommand(order.getId(), "payment-key-1",
                new BigDecimal("8.00"), new BigDecimal("22.00"));
        PaymentApplicationService service = service();

        when(paymentTransactionRepository.findByIdempotencyKey(command.idempotencyKey())).thenReturn(Optional.empty());
        when(customerOrderRepository.findByIdWithSubOrdersForUpdate(order.getId())).thenReturn(Optional.of(order));
        when(paymentTransactionRepository.findFirstByOrderIdOrderByCreatedAtDesc(order.getId())).thenReturn(Optional.empty());
        when(walletAccountRepository.findByCustomerIdForUpdate(order.getCustomerId())).thenReturn(Optional.of(wallet));
        when(paymentProviderGateway.createCardPayment(any(), any()))
                .thenReturn(new PaymentProviderGateway.ProviderPayment(UUID.randomUUID(),
                        com.nexamarket.nexamarket.payment.domain.ProviderPaymentStatus.PENDING));
        when(paymentTransactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentView result = service.initiate(command);

        assertThat(result.status()).isEqualTo(PaymentStatus.PENDING.name());
        assertThat(result.walletAmount()).isEqualByComparingTo("8.00");
        assertThat(result.cardAmount()).isEqualByComparingTo("22.00");
        assertThat(result.providerPaymentId()).isNotNull();
        assertThat(wallet.getBalance()).isEqualByComparingTo("4.00");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
        verify(paymentProviderGateway).createCardPayment(result.paymentId(), new BigDecimal("22.00"));
    }

    @Test
    void confirmsOrderImmediatelyWhenWalletCoversTheFullAmount() {
        CustomerOrder order = order("30.00");
        WalletAccount wallet = WalletAccount.open(order.getCustomerId(), new BigDecimal("30.00"));
        InitiatePaymentCommand command = new InitiatePaymentCommand(order.getId(), "payment-key-2",
                new BigDecimal("30.00"), BigDecimal.ZERO);
        PaymentApplicationService service = service();

        when(paymentTransactionRepository.findByIdempotencyKey(command.idempotencyKey())).thenReturn(Optional.empty());
        when(customerOrderRepository.findByIdWithSubOrdersForUpdate(order.getId())).thenReturn(Optional.of(order));
        when(paymentTransactionRepository.findFirstByOrderIdOrderByCreatedAtDesc(order.getId())).thenReturn(Optional.empty());
        when(walletAccountRepository.findByCustomerIdForUpdate(order.getCustomerId())).thenReturn(Optional.of(wallet));
        when(paymentTransactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentView result = service.initiate(command);

        assertThat(result.status()).isEqualTo(PaymentStatus.SUCCEEDED.name());
        assertThat(result.providerPaymentId()).isNull();
        assertThat(wallet.getBalance()).isZero();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getSubOrders()).allSatisfy(subOrder -> assertThat(subOrder.getStatus()).isEqualTo(OrderStatus.PAID));
        verify(paymentProviderGateway, never()).createCardPayment(any(), any());
    }

    @Test
    void returnsTheOriginalPaymentForAnIdenticalIdempotencyRetry() {
        CustomerOrder order = order("30.00");
        InitiatePaymentCommand command = new InitiatePaymentCommand(order.getId(), "payment-key-3",
                BigDecimal.ZERO, new BigDecimal("30.00"));
        var original = com.nexamarket.nexamarket.payment.domain.PaymentTransaction.initiate(order.getId(),
                order.getCustomerId(), command.idempotencyKey(), BigDecimal.ZERO, new BigDecimal("30.00"), Instant.now());
        PaymentApplicationService service = service();
        when(paymentTransactionRepository.findByIdempotencyKey(command.idempotencyKey())).thenReturn(Optional.of(original));

        PaymentView result = service.initiate(command);

        assertThat(result.paymentId()).isEqualTo(original.getId());
        verify(customerOrderRepository, never()).findByIdWithSubOrdersForUpdate(any());
        verify(paymentProviderGateway, never()).createCardPayment(any(), any());
    }

    private PaymentApplicationService service() {
        return new PaymentApplicationService(customerOrderRepository, paymentTransactionRepository,
                walletAccountRepository, paymentProviderGateway, Duration.ofSeconds(30),
                Clock.fixed(Instant.parse("2026-08-26T09:00:00Z"), ZoneOffset.UTC));
    }

    private CustomerOrder order(String amount) {
        return CustomerOrder.from(new CheckoutOrderRequest(UUID.randomUUID(), UUID.randomUUID(), List.of(
                new CheckoutOrderRequest.SellerOrderRequest(UUID.randomUUID(), List.of(
                        new CheckoutOrderRequest.OrderItemRequest(UUID.randomUUID(), 1, new BigDecimal(amount),
                                UUID.randomUUID(), Instant.parse("2026-08-26T12:00:00Z")))))));
    }
}
