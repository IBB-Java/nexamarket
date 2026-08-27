package com.nexamarket.nexamarket.cart.application;

public record AddCartItemCommand(Long customerId, Long productVariantId, int quantity) {
}
