package com.nexamarket.nexamarket.order.api;

import com.nexamarket.nexamarket.order.domain.InvalidOrderStateTransitionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class OrderExceptionHandler {

    @ExceptionHandler(InvalidOrderStateTransitionException.class)
    public ProblemDetail handleInvalidStateTransition(InvalidOrderStateTransitionException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problemDetail.setTitle("Invalid order status transition");
        return problemDetail;
    }
}
