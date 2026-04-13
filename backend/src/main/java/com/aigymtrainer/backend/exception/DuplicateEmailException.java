package com.aigymtrainer.backend.exception;

public class DuplicateEmailException extends BaseException {
    public DuplicateEmailException(String email) {
        super("DUPLICATE_EMAIL", "Email '" + email + "' is already registered");
    }
}
