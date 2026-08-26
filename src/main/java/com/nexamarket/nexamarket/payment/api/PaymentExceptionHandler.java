package com.nexamarket.nexamarket.payment.api;

import com.nexamarket.nexamarket.payment.application.PaymentException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PaymentExceptionHandler {

    @ExceptionHandler(PaymentException.class)
    public ProblemDetail handlePaymentException(PaymentException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
        problemDetail.setTitle("Payment request could not be completed");
        return problemDetail;
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ProblemDetail handleInvalidPaymentRequest(RuntimeException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problemDetail.setTitle("Invalid payment request");
        return problemDetail;
    }
}
