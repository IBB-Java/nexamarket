package com.nexamarket.users.application;

public class InvalidSellerReviewException extends RuntimeException {
    public InvalidSellerReviewException() { super("Satıcı incelemesinde yalnızca ACTIVE, REJECTED veya SUSPENDED seçilebilir"); }
}
