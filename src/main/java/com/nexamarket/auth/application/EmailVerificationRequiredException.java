package com.nexamarket.auth.application;

public class EmailVerificationRequiredException extends RuntimeException {
    public EmailVerificationRequiredException() {
        super("Giriş yapmadan önce e-posta adresine gönderilen doğrulama bağlantısını açmalısın.");
    }
}
