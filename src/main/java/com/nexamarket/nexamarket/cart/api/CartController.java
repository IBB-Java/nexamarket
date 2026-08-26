package com.nexamarket.nexamarket.cart.api;

import com.nexamarket.nexamarket.cart.application.AddCartItemCommand;
import com.nexamarket.nexamarket.cart.application.CartApplicationService;
import com.nexamarket.nexamarket.cart.application.CartCheckoutService;
import com.nexamarket.nexamarket.cart.application.CartView;
import com.nexamarket.nexamarket.cart.application.CheckoutCartCommand;
import com.nexamarket.nexamarket.cart.application.CheckoutCartView;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart/items")
public class CartController {

    private final CartApplicationService cartApplicationService;
    private final CartCheckoutService cartCheckoutService;

    public CartController(CartApplicationService cartApplicationService, CartCheckoutService cartCheckoutService) {
        this.cartApplicationService = cartApplicationService;
        this.cartCheckoutService = cartCheckoutService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CartView addItem(@Valid @RequestBody AddCartItemRequest request) {
        return cartApplicationService.addItem(new AddCartItemCommand(
                request.customerId(), request.productVariantId(), request.sellerId(), request.quantity()));
    }

    @PostMapping("/checkout")
    public CheckoutCartView checkout(@Valid @RequestBody CheckoutCartRequest request) {
        return cartCheckoutService.checkout(new CheckoutCartCommand(request.customerId()));
    }
}
