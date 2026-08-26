package com.nexamarket.nexamarket.payment.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record WalletCreditRequest(@NotNull @DecimalMin(value = "0.01") BigDecimal amount) {
}
