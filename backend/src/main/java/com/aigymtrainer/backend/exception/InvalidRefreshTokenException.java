package com.aigymtrainer.backend.exception;

public class InvalidRefreshTokenException extends BaseException {
    public InvalidRefreshTokenException(String message) {
        super("INVALID_REFRESH_TOKEN", message);
    }
}
