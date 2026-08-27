package com.nexamarket.users.application;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long userId) { super("Kullanıcı bulunamadı: " + userId); }
}
