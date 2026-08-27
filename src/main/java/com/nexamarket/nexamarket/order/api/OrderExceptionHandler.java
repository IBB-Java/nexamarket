package com.nexamarket.nexamarket.order.api;

import com.nexamarket.nexamarket.order.domain.InvalidOrderStateTransitionException;
import com.nexamarket.nexamarket.order.application.OrderAccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestControllerAdvice
public class OrderExceptionHandler {

    @ExceptionHandler(OrderAccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ProblemDetail accessDenied(OrderAccessDeniedException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(InvalidOrderStateTransitionException.class)
    public ProblemDetail handleInvalidStateTransition(InvalidOrderStateTransitionException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problemDetail.setTitle("Invalid order status transition");
        return problemDetail;
    }
}
