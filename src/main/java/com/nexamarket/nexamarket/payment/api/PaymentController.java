package com.nexamarket.nexamarket.payment.api;

import com.nexamarket.nexamarket.payment.application.InitiatePaymentCommand;
import com.nexamarket.nexamarket.payment.application.PaymentApplicationService;
import com.nexamarket.nexamarket.payment.application.PaymentView;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentApplicationService paymentApplicationService;

    public PaymentController(PaymentApplicationService paymentApplicationService) {
        this.paymentApplicationService = paymentApplicationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentView initiate(@Valid @RequestBody InitiatePaymentRequest request) {
        return paymentApplicationService.initiate(new InitiatePaymentCommand(request.orderId(), request.idempotencyKey(),
                request.walletAmount(), request.cardAmount()));
    }
}
