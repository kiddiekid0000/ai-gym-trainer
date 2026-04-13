package com.aigymtrainer.backend.common.exception;

public class TokenBlacklistException extends BaseException {
    public TokenBlacklistException(String message) {
        super("TOKEN_BLACKLIST_ERROR", message);
    }
}
