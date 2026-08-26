package com.nexamarket.nexamarket.cart.api;

import com.nexamarket.nexamarket.cart.application.AddCartItemCommand;
import com.nexamarket.nexamarket.cart.application.CartApplicationService;
import com.nexamarket.nexamarket.cart.application.CartView;
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

    public CartController(CartApplicationService cartApplicationService) {
        this.cartApplicationService = cartApplicationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CartView addItem(@Valid @RequestBody AddCartItemRequest request) {
        return cartApplicationService.addItem(new AddCartItemCommand(
                request.customerId(), request.productVariantId(), request.sellerId(), request.quantity()));
    }
}
