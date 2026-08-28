package com.nexamarket.nexamarket.cart.api;

import com.nexamarket.nexamarket.cart.application.AddCartItemCommand;
import com.nexamarket.nexamarket.cart.application.CartApplicationService;
import com.nexamarket.nexamarket.cart.application.CartCheckoutService;
import com.nexamarket.nexamarket.cart.application.CartView;
import com.nexamarket.nexamarket.cart.application.CheckoutCartCommand;
import com.nexamarket.nexamarket.cart.application.CheckoutCartView;
import com.nexamarket.auth.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

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
    public CartView addItem(@AuthenticationPrincipal AuthPrincipal principal,
                            @Valid @RequestBody AddCartItemRequest request) {
        return cartApplicationService.addItem(new AddCartItemCommand(
                principal.userId(), request.productVariantId(), request.quantity()));
    }

    @GetMapping
    public CartView getActiveCart(@AuthenticationPrincipal AuthPrincipal principal) {
        return cartApplicationService.getActiveCart(principal.userId());
    }

    @DeleteMapping("/{cartItemId}")
    public CartView removeItem(@AuthenticationPrincipal AuthPrincipal principal,
                               @PathVariable UUID cartItemId) {
        return cartApplicationService.removeItem(principal.userId(), cartItemId);
    }

    @PostMapping("/checkout")
    public CheckoutCartView checkout(@AuthenticationPrincipal AuthPrincipal principal,
                                     @RequestBody(required = false) CheckoutCartRequest request) {
        return cartCheckoutService.checkout(new CheckoutCartCommand(principal.userId(),
                request == null ? java.util.List.of() : request.promotionCodes()));
    }
}
