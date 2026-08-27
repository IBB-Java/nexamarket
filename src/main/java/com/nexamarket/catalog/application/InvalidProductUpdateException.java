package com.nexamarket.catalog.application;

public class InvalidProductUpdateException extends RuntimeException {
    public InvalidProductUpdateException(String message) {
        super(message);
    }
}
