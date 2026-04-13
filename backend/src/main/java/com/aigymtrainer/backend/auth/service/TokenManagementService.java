package com.aigymtrainer.backend.auth.service;

import com.aigymtrainer.backend.auth.dto.AuthResult;

public interface TokenManagementService {
    AuthResult refreshAccessToken(String refreshToken);
    void logout(String refreshToken, String accessToken);
}
