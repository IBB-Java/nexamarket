package com.nexamarket.auth.application;

public class InvalidRegistrationRoleException extends RuntimeException {

    public InvalidRegistrationRoleException(String message) {
        super(message);
    }
}
