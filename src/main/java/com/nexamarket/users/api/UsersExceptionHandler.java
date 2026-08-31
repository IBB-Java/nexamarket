package com.nexamarket.users.api;

import com.nexamarket.users.application.InvalidSellerReviewException;
import com.nexamarket.users.application.InvalidUserRoleChangeException;
import com.nexamarket.users.application.InvalidUserStatusChangeException;
import com.nexamarket.users.application.SellerAccessDeniedException;
import com.nexamarket.users.application.SellerProfileConflictException;
import com.nexamarket.users.application.SellerProfileNotFoundException;
import com.nexamarket.users.application.UserNotFoundException;
import com.nexamarket.users.application.UserAccountUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class UsersExceptionHandler {

    @ExceptionHandler({UserNotFoundException.class, SellerProfileNotFoundException.class})
    ProblemDetail notFound(RuntimeException exception) {
        return problem(HttpStatus.NOT_FOUND, "Kaynak bulunamadı", exception.getMessage());
    }

    @ExceptionHandler(SellerProfileConflictException.class)
    ProblemDetail conflict(SellerProfileConflictException exception) {
        return problem(HttpStatus.CONFLICT, "Satıcı profili çakışması", exception.getMessage());
    }

    @ExceptionHandler({SellerAccessDeniedException.class, UserAccountUnavailableException.class})
    ProblemDetail forbidden(RuntimeException exception) {
        return problem(HttpStatus.FORBIDDEN, "İşlem yetkisi yok", exception.getMessage());
    }

    @ExceptionHandler(InvalidSellerReviewException.class)
    ProblemDetail invalidReview(InvalidSellerReviewException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Geçersiz satıcı incelemesi", exception.getMessage());
    }

    @ExceptionHandler(InvalidUserStatusChangeException.class)
    ProblemDetail invalidUserStatus(InvalidUserStatusChangeException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Geçersiz kullanıcı durumu", exception.getMessage());
    }

    @ExceptionHandler(InvalidUserRoleChangeException.class)
    ProblemDetail invalidUserRole(InvalidUserRoleChangeException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Geçersiz kullanıcı rolü", exception.getMessage());
    }

    private ProblemDetail problem(HttpStatus status, String title, String message) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setTitle(title);
        detail.setType(URI.create("https://nexamarket.local/problems/users/" + status.value()));
        return detail;
    }
}
