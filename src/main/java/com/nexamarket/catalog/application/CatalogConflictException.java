package com.nexamarket.catalog.application;

public class CatalogConflictException extends RuntimeException {
    public CatalogConflictException(String message) {
        super(message);
    }
}
