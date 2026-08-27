package com.nexamarket.auth.application;

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException() { super("Geçersiz veya süresi dolmuş token"); }
}
