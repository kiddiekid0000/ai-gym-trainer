package com.aigymtrainer.backend.common.exception;

public class InvalidCredentialsException extends BaseException {
    public InvalidCredentialsException(String message) {
        super("INVALID_CREDENTIALS", message);
    }
}
