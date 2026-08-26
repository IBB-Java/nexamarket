package com.nexamarket.nexamarket.payment.api;

import com.nexamarket.nexamarket.payment.application.PaymentView;
import com.nexamarket.nexamarket.payment.application.PaymentWebhookCommand;
import com.nexamarket.nexamarket.payment.application.PaymentWebhookService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments/webhooks")
public class PaymentWebhookController {

    private final PaymentWebhookService paymentWebhookService;

    public PaymentWebhookController(PaymentWebhookService paymentWebhookService) {
        this.paymentWebhookService = paymentWebhookService;
    }

    @PostMapping("/mock")
    public PaymentView receiveMockWebhook(@Valid @RequestBody PaymentWebhookRequest request) {
        return paymentWebhookService.handle(new PaymentWebhookCommand(request.providerEventId(), request.providerPaymentId(),
                request.status(), request.failureReason()));
    }
}
