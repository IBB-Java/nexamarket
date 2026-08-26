package com.nexamarket.common.api;

import com.nexamarket.catalog.application.CatalogConflictException;
import com.nexamarket.catalog.application.CatalogNotFoundException;
import com.nexamarket.catalog.application.InvalidProductImageException;
import com.nexamarket.catalog.application.ThumbnailNotReadyException;
import com.nexamarket.catalog.storage.ObjectStorageException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(CatalogNotFoundException.class)
    ProblemDetail handleNotFound(CatalogNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Kaynak bulunamadı", exception.getMessage());
    }

    @ExceptionHandler(CatalogConflictException.class)
    ProblemDetail handleConflict(CatalogConflictException exception) {
        return problem(HttpStatus.CONFLICT, "Katalog çakışması", exception.getMessage());
    }

    @ExceptionHandler(InvalidProductImageException.class)
    ProblemDetail handleInvalidImage(InvalidProductImageException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Geçersiz ürün görseli", exception.getMessage());
    }

    @ExceptionHandler(ThumbnailNotReadyException.class)
    ProblemDetail handleThumbnailNotReady(ThumbnailNotReadyException exception) {
        return problem(HttpStatus.TOO_EARLY, "Thumbnail hazır değil", exception.getMessage());
    }

    @ExceptionHandler(ObjectStorageException.class)
    ProblemDetail handleStorage(ObjectStorageException exception) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Nesne depolama kullanılamıyor", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        ProblemDetail detail = problem(HttpStatus.BAD_REQUEST, "Doğrulama hatası", "İstek alanlarını kontrol edin.");
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        detail.setProperty("errors", errors);
        return detail;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(ConstraintViolationException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Doğrulama hatası", exception.getMessage());
    }

    private ProblemDetail problem(HttpStatus status, String title, String detailMessage) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, detailMessage);
        detail.setTitle(title);
        detail.setType(URI.create("https://nexamarket.local/problems/" + status.value()));
        return detail;
    }
}
