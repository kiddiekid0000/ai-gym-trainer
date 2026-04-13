package com.aigymtrainer.backend.auth.strategy;

import com.aigymtrainer.backend.auth.dto.AuthResult;
import com.aigymtrainer.backend.auth.dto.AuthTokens;
import com.aigymtrainer.backend.auth.dto.LoginRequest;
import com.aigymtrainer.backend.auth.service.TokenManagementService;
import com.aigymtrainer.backend.common.exception.AccountNotVerifiedException;
import com.aigymtrainer.backend.common.exception.AccountSuspendedException;
import com.aigymtrainer.backend.common.exception.InvalidCredentialsException;
import com.aigymtrainer.backend.common.exception.UserNotFoundException;
import com.aigymtrainer.backend.security.jwt.JwtService;
import com.aigymtrainer.backend.user.domain.Status;
import com.aigymtrainer.backend.user.domain.User;
import com.aigymtrainer.backend.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UserAuthenticationStrategy implements AuthenticationStrategy {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenManagementService tokenManagementService;

    public UserAuthenticationStrategy(UserRepository userRepository,
                                      PasswordEncoder passwordEncoder,
                                      JwtService jwtService,
                                      TokenManagementService tokenManagementService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenManagementService = tokenManagementService;
    }

    @Override
    public AuthResult authenticate(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new UserNotFoundException(loginRequest.getEmail()));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
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
        
        // Store refresh token in Redis (delegated to TokenManagementService)
        // This is handled after the strategy returns

        return new AuthResult(new AuthTokens(accessToken, refreshToken), user);
    }

    @Override
    public boolean supports(String role) {
        return "USER".equals(role);
    }
}
