package com.nexamarket.users.application;

public class UserDeletionNotAllowedException extends RuntimeException {
    public UserDeletionNotAllowedException(String message) {
        super(message);
    }
}
