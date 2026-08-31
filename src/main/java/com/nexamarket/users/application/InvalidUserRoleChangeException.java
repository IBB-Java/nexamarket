package com.nexamarket.users.application;

public class InvalidUserRoleChangeException extends RuntimeException {
    public InvalidUserRoleChangeException() {
        super("Yalnızca CUSTOMER rolündeki kullanıcı SELLER rolüne yükseltilebilir");
    }
}
