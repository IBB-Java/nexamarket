package com.nexamarket.users.application;

public class InvalidUserStatusChangeException extends RuntimeException {
    public InvalidUserStatusChangeException() { super("Yalnızca ACTIVE veya DISABLED kullanıcı durumu atanabilir"); }

    public InvalidUserStatusChangeException(String message) { super(message); }
}
