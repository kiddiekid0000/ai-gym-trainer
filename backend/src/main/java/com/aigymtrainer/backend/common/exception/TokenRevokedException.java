package com.aigymtrainer.backend.common.exception;

public class TokenRevokedException extends BaseException {
    public TokenRevokedException(String message) {
        super("TOKEN_REVOKED", message);
    }
}
