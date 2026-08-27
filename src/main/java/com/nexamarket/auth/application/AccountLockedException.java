package com.nexamarket.auth.application;

import java.time.Instant;

public class AccountLockedException extends RuntimeException {
    public AccountLockedException(Instant lockedUntil) {
        super("Hesap geçici olarak kilitlendi. Kilit bitiş zamanı: " + lockedUntil);
    }
}
