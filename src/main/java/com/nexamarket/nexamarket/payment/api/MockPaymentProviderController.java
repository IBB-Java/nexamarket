package com.nexamarket.nexamarket.payment.api;

import com.nexamarket.nexamarket.payment.mock.MockPaymentProviderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/mock-payment-provider/payments")
public class MockPaymentProviderController {

    private final MockPaymentProviderService mockPaymentProviderService;

    public MockPaymentProviderController(MockPaymentProviderService mockPaymentProviderService) {
        this.mockPaymentProviderService = mockPaymentProviderService;
    }

    @PostMapping("/{providerPaymentId}/outcomes")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void scheduleOutcome(@PathVariable UUID providerPaymentId,
                                @Valid @RequestBody MockProviderOutcomeRequest request) {
        mockPaymentProviderService.setOutcomeAndScheduleCallback(providerPaymentId, request.status(),
                request.failureReason(), Duration.ofSeconds(request.callbackDelaySeconds()), request.duplicateDeliveries());
    }
}
