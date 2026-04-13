package com.aigymtrainer.backend.auth.strategy;

import com.aigymtrainer.backend.auth.dto.AuthResult;
import com.aigymtrainer.backend.auth.dto.AuthTokens;
import com.aigymtrainer.backend.auth.dto.LoginRequest;
import com.aigymtrainer.backend.common.exception.InvalidCredentialsException;
import com.aigymtrainer.backend.security.jwt.JwtService;
import com.aigymtrainer.backend.user.domain.Role;
import com.aigymtrainer.backend.user.domain.Status;
import com.aigymtrainer.backend.user.domain.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AdminAuthenticationStrategy implements AuthenticationStrategy {

    private final JwtService jwtService;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password}")
    private String adminPassword;

    public AdminAuthenticationStrategy(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public AuthResult authenticate(LoginRequest loginRequest) {
        if (!loginRequest.getEmail().equals(adminEmail) || !loginRequest.getPassword().equals(adminPassword)) {
            throw new InvalidCredentialsException("Invalid admin credentials");
        }

        // Create admin user object
        User adminUser = new User();
        adminUser.setEmail(adminEmail);
        adminUser.setRole(Role.ADMIN);
        adminUser.setStatus(Status.ACTIVE);
        adminUser.setVerified(true);

        String accessToken = jwtService.generateAccessToken(adminEmail, Role.ADMIN.name());
        String refreshToken = jwtService.generateRefreshToken(adminEmail, Role.ADMIN.name());

        return new AuthResult(new AuthTokens(accessToken, refreshToken), adminUser);
    }

    @Override
    public boolean supports(String role) {
        return "ADMIN".equals(role);
    }
}
