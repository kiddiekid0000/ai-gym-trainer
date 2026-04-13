package com.aigymtrainer.backend.auth.service;

import com.aigymtrainer.backend.auth.dto.AuthResult;
import com.aigymtrainer.backend.auth.dto.LoginRequest;

public interface AuthenticationService {
    AuthResult authenticate(LoginRequest loginRequest);
}
