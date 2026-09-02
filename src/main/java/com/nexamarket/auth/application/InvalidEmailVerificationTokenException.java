package com.nexamarket.auth.application;

public class InvalidEmailVerificationTokenException extends RuntimeException {
    public InvalidEmailVerificationTokenException() {
        super("Doğrulama bağlantısı geçersiz veya süresi dolmuş. Yeni bir bağlantı isteyebilirsin.");
    }
}
