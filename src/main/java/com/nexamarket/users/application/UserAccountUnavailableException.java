package com.nexamarket.users.application;

public class UserAccountUnavailableException extends RuntimeException {
    public UserAccountUnavailableException() { super("Kullanıcı hesabı aktif değil"); }
}
