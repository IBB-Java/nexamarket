package com.nexamarket.stock.api;

import com.nexamarket.stock.application.InsufficientStockException;
import com.nexamarket.stock.application.InvalidReservationStateException;
import com.nexamarket.stock.application.StockAccessDeniedException;
import com.nexamarket.stock.application.StockReservationNotFoundException;
import com.nexamarket.stock.application.StockVariantNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class StockExceptionHandler {

    @ExceptionHandler({StockVariantNotFoundException.class, StockReservationNotFoundException.class})
    ProblemDetail notFound(RuntimeException exception) {
        return problem(HttpStatus.NOT_FOUND, "Stok kaynağı bulunamadı", exception.getMessage());
    }

    @ExceptionHandler(InsufficientStockException.class)
    ProblemDetail insufficientStock(InsufficientStockException exception) {
        return problem(HttpStatus.CONFLICT, "Yetersiz stok", exception.getMessage());
    }

    @ExceptionHandler(InvalidReservationStateException.class)
    ProblemDetail invalidState(InvalidReservationStateException exception) {
        return problem(HttpStatus.CONFLICT, "Geçersiz rezervasyon durumu", exception.getMessage());
    }

    @ExceptionHandler(StockAccessDeniedException.class)
    ProblemDetail forbidden(StockAccessDeniedException exception) {
        return problem(HttpStatus.FORBIDDEN, "Stok işlemi yetkisiz", exception.getMessage());
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("https://nexamarket.local/problems/stock/" + status.value()));
        return problem;
    }
}
