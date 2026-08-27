package com.nexamarket.auth.application;

public class AccountDisabledException extends RuntimeException {
    public AccountDisabledException() { super("Hesap kullanıma kapalı"); }
}
