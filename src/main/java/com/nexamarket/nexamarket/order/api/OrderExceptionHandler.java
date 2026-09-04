package com.nexamarket.nexamarket.order.api;

import com.nexamarket.nexamarket.order.domain.InvalidOrderStateTransitionException;
import com.nexamarket.nexamarket.order.application.OrderAccessDeniedException;
import com.nexamarket.nexamarket.order.application.InvalidCourierAssignmentException;
import com.nexamarket.nexamarket.order.application.OrderNotFoundException;
import com.nexamarket.nexamarket.order.application.DeliveryAssignmentNotFoundException;
import com.nexamarket.nexamarket.order.domain.InvalidDeliveryStateTransitionException;
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

    @ExceptionHandler(OrderNotFoundException.class)
    public ProblemDetail notFound(OrderNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(InvalidCourierAssignmentException.class)
    public ProblemDetail invalidCourierAssignment(InvalidCourierAssignmentException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(DeliveryAssignmentNotFoundException.class)
    public ProblemDetail deliveryNotFound(DeliveryAssignmentNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(InvalidDeliveryStateTransitionException.class)
    public ProblemDetail invalidDeliveryTransition(InvalidDeliveryStateTransitionException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Geçersiz teslimat durumu geçişi");
        return problem;
    }

    @ExceptionHandler(InvalidOrderStateTransitionException.class)
    public ProblemDetail handleInvalidStateTransition(InvalidOrderStateTransitionException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problemDetail.setTitle("Invalid order status transition");
        return problemDetail;
    }
}
