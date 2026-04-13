package com.aigymtrainer.backend.exception;

public class AccountSuspendedException extends BaseException {
    public AccountSuspendedException(String email) {
        super("ACCOUNT_SUSPENDED", "Account '" + email + "' has been suspended");
    }
}
