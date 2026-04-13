package com.aigymtrainer.backend.exception;

public class InvalidCredentialsException extends BaseException {
    public InvalidCredentialsException(String message) {
        super("INVALID_CREDENTIALS", message);
    }
}
