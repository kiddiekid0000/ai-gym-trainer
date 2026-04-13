package com.aigymtrainer.backend.auth.service.impl;

import com.aigymtrainer.backend.auth.dto.AuthResult;
import com.aigymtrainer.backend.auth.dto.LoginRequest;
import com.aigymtrainer.backend.auth.service.AuthenticationService;
import com.aigymtrainer.backend.auth.strategy.AuthenticationStrategy;
import com.aigymtrainer.backend.auth.strategy.AuthenticationStrategyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationServiceImpl.class);

    private final AuthenticationStrategyFactory strategyFactory;
    private final TokenManagementServiceImpl tokenManagementService;

    public AuthenticationServiceImpl(AuthenticationStrategyFactory strategyFactory,
                                    TokenManagementServiceImpl tokenManagementService) {
        this.strategyFactory = strategyFactory;
        this.tokenManagementService = tokenManagementService;
    }

    @Override
    public AuthResult authenticate(LoginRequest loginRequest) {
        logger.debug("Authenticating user: {}", loginRequest.getEmail());

        // Get the appropriate strategy
        AuthenticationStrategy strategy = strategyFactory.getStrategy(loginRequest);

        // Authenticate using the strategy
        AuthResult authResult = strategy.authenticate(loginRequest);

        // Store refresh token in Redis
        if (authResult.tokens() != null) {
            tokenManagementService.storeRefreshToken(
                    authResult.user().getEmail(),
                    authResult.tokens().getRefreshToken()
            );
        }

        logger.info("User authenticated successfully: {}", loginRequest.getEmail());
        return authResult;
    }
}
