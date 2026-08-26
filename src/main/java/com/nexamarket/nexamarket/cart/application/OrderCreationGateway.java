package com.nexamarket.nexamarket.cart.application;

/** Boundary for creating the parent order and seller-specific sub-orders. */
public interface OrderCreationGateway {

    OrderCreation createOrder(CheckoutOrderRequest request);
}
