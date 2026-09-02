package com.nexamarket.users.application;

public class InvalidUserRoleChangeException extends RuntimeException {
    public InvalidUserRoleChangeException() {
        super("Yalnızca CUSTOMER, SELLER ve COURIER rolleri birbirine dönüştürülebilir");
    }
}
