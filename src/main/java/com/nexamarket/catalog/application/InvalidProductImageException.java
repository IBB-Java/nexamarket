package com.nexamarket.catalog.application;

public class InvalidProductImageException extends RuntimeException {
    public InvalidProductImageException(String message) {
        super(message);
    }

    public InvalidProductImageException(String message, Throwable cause) {
        super(message, cause);
    }
}
