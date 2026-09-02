package com.nexamarket.auth.application;

public class InvalidEmailVerificationTokenException extends RuntimeException {
    public InvalidEmailVerificationTokenException() {
        super("Doğrulama kodu geçersiz veya süresi dolmuş. Yeni bir kod isteyebilirsin.");
    }
}
