package com.aigymtrainer.backend.auth.dto;

import com.aigymtrainer.backend.user.User;

public record AuthResult(AuthTokens tokens, User user) {
}