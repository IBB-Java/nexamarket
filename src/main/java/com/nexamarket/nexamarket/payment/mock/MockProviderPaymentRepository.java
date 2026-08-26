package com.nexamarket.nexamarket.payment.mock;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MockProviderPaymentRepository extends JpaRepository<MockProviderPayment, UUID> {
}
