package com.nexamarket.users.application;

public class SellerProfileNotFoundException extends RuntimeException {
    public SellerProfileNotFoundException(Long sellerId) { super("Satıcı profili bulunamadı: " + sellerId); }
}
