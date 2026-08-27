package com.nexamarket.auth.api;

import com.nexamarket.auth.application.AccountDisabledException;
import com.nexamarket.auth.application.AccountLockedException;
import com.nexamarket.auth.application.DuplicateEmailException;
import com.nexamarket.auth.application.InvalidCredentialsException;
import com.nexamarket.auth.application.InvalidTokenException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(DuplicateEmailException.class)
    ProblemDetail duplicateEmail(DuplicateEmailException exception) {
        return problem(HttpStatus.CONFLICT, "Kayıt çakışması", exception.getMessage());
    }

    @ExceptionHandler({InvalidCredentialsException.class, InvalidTokenException.class})
    ProblemDetail unauthorized(RuntimeException exception) {
        return problem(HttpStatus.UNAUTHORIZED, "Kimlik doğrulama başarısız", exception.getMessage());
    }

    @ExceptionHandler(AccountLockedException.class)
    ProblemDetail locked(AccountLockedException exception) {
        return problem(HttpStatus.LOCKED, "Hesap geçici olarak kilitli", exception.getMessage());
    }

    @ExceptionHandler(AccountDisabledException.class)
    ProblemDetail disabled(AccountDisabledException exception) {
        return problem(HttpStatus.FORBIDDEN, "Hesap kullanıma kapalı", exception.getMessage());
    }

    private ProblemDetail problem(HttpStatus status, String title, String message) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setTitle(title);
        detail.setType(URI.create("https://nexamarket.local/problems/auth/" + status.value()));
        return detail;
    }
}
