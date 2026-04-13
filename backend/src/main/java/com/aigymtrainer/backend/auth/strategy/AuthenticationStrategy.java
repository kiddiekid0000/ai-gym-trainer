package com.aigymtrainer.backend.auth.strategy;

import com.aigymtrainer.backend.auth.dto.AuthResult;
import com.aigymtrainer.backend.auth.dto.LoginRequest;

public interface AuthenticationStrategy {
    AuthResult authenticate(LoginRequest loginRequest);
    boolean supports(String role);
}
