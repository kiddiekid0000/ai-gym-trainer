package com.aigymtrainer.backend.auth.dto;

import com.aigymtrainer.backend.user.domain.User;

public record AuthResult(AuthTokens tokens, User user) {}
