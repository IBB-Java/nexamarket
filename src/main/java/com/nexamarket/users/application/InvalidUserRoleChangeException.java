package com.nexamarket.users.application;

public class InvalidUserRoleChangeException extends RuntimeException {
    public InvalidUserRoleChangeException() {
        super("Bu kullanıcı için rol ataması yapılamaz");
    }

    public InvalidUserRoleChangeException(String message) { super(message); }
}
