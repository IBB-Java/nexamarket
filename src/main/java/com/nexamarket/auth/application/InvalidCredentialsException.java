package com.nexamarket.auth.application;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() { super("E-posta veya şifre hatalı"); }
}
