package com.aigymtrainer.backend.common.exception;

public class DuplicateEmailException extends BaseException {
    public DuplicateEmailException(String email) {
        super("DUPLICATE_EMAIL", "Email '" + email + "' is already registered");
    }
}
