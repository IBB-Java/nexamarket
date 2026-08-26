package com.nexamarket.catalog.application;

public class ThumbnailNotReadyException extends RuntimeException {
    public ThumbnailNotReadyException(String message) {
        super(message);
    }
}
