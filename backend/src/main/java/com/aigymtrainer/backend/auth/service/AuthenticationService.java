package com.aigymtrainer.backend.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.aigymtrainer.backend.auth.dto.AuthResult;
import com.aigymtrainer.backend.auth.dto.AuthTokens;
import com.aigymtrainer.backend.auth.dto.LoginRequest;
import com.aigymtrainer.backend.exception.AccountNotVerifiedException;
import com.aigymtrainer.backend.exception.AccountSuspendedException;
import com.aigymtrainer.backend.exception.InvalidCredentialsException;
import com.aigymtrainer.backend.security.service.JwtService;
import com.aigymtrainer.backend.user.domain.Role;
import com.aigymtrainer.backend.user.domain.Status;
import com.aigymtrainer.backend.user.domain.User;
import com.aigymtrainer.backend.user.service.UserService;

@Service
public class AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenService tokenService;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password}")
    private String adminPassword;

    public AuthenticationService(UserService userService,
                                    PasswordEncoder passwordEncoder,
                                    JwtService jwtService,
                                    TokenService tokenService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenService = tokenService;
    }

    public AuthResult authenticate(LoginRequest loginRequest) {
        logger.debug("Authenticating user: {}", loginRequest.email());

        // Check if it's admin authentication
        if (loginRequest.email().equals(adminEmail)) {
            return authenticateAdmin(loginRequest);
        }

        // Otherwise, authenticate as regular user
        return authenticateUser(loginRequest);
    }

    private AuthResult authenticateAdmin(LoginRequest loginRequest) {
        if (!loginRequest.password().equals(adminPassword)) {
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

        // Store refresh token in Redis
        tokenService.storeRefreshToken(adminEmail, refreshToken);

        logger.info("Admin authenticated successfully");
        return new AuthResult(new AuthTokens(accessToken, refreshToken), adminUser);
    }

    private AuthResult authenticateUser(LoginRequest loginRequest) {
        User user = userService.findByEmail(loginRequest.email());

        if (!passwordEncoder.matches(loginRequest.password(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid password");
        }

        if (!user.isVerified()) {
            throw new AccountNotVerifiedException(user.getEmail());
        }

        if (user.getStatus() == Status.SUSPENDED) {
            throw new AccountSuspendedException(user.getEmail());
        }

        String accessToken = jwtService.generateAccessToken(user.getEmail(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail(), user.getRole().name());

        // Store refresh token in Redis
        tokenService.storeRefreshToken(user.getEmail(), refreshToken);

        logger.info("User authenticated successfully: {}", loginRequest.email());
        return new AuthResult(new AuthTokens(accessToken, refreshToken), user);
    }
}
