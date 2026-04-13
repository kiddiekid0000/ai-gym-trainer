package com.aigymtrainer.backend.auth.strategy;

import com.aigymtrainer.backend.auth.dto.LoginRequest;
import com.aigymtrainer.backend.common.exception.InvalidCredentialsException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class AuthenticationStrategyFactory {

    private final Map<String, AuthenticationStrategy> strategies = new HashMap<>();

    public AuthenticationStrategyFactory(Set<AuthenticationStrategy> implementers) {
        implementers.forEach(strategy -> strategies.put(strategy.getClass().getSimpleName(), strategy));
    }

    public AuthenticationStrategy getStrategy(LoginRequest loginRequest) {
        // Try to determine if it's admin or user based on email + strategy matching
        for (AuthenticationStrategy strategy : strategies.values()) {
            if (strategy instanceof AdminAuthenticationStrategy && 
                loginRequest.getEmail().equals("admin@example.com")) { // Adjust as needed
                return strategy;
            }
        }
        
        // Default to user authentication strategy
        return strategies.get("UserAuthenticationStrategy");
    }
}
