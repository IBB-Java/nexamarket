package com.nexamarket.users.application;

public class SellerAccessDeniedException extends RuntimeException {
    public SellerAccessDeniedException() { super("Bu işlem yalnızca SELLER rolündeki kullanıcılar içindir"); }
}
