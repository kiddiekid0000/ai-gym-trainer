package com.aigymtrainer.backend.exception;

public class AccountNotVerifiedException extends BaseException {
    public AccountNotVerifiedException(String email) {
        super("ACCOUNT_NOT_VERIFIED", "Account '" + email + "' is not verified. Please verify your email with OTP first");
    }
}
