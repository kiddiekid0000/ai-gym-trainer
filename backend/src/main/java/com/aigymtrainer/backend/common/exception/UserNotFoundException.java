package com.aigymtrainer.backend.common.exception;

public class UserNotFoundException extends BaseException {
    public UserNotFoundException(String email) {
        super("USER_NOT_FOUND", "User with email '" + email + "' not found");
    }

    public UserNotFoundException(Long id) {
        super("USER_NOT_FOUND", "User with ID '" + id + "' not found");
    }
}
