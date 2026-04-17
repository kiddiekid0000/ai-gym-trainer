package com.aigymtrainer.backend.auth.service;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aigymtrainer.backend.auth.dto.AuthResult;
import com.aigymtrainer.backend.common.constant.RedisKeyConstants;
import com.aigymtrainer.backend.exception.AccountSuspendedException;
import com.aigymtrainer.backend.exception.InvalidRefreshTokenException;
import com.aigymtrainer.backend.exception.TokenRevokedException;
import com.aigymtrainer.backend.security.service.JwtService;
import com.aigymtrainer.backend.user.domain.User;
import com.aigymtrainer.backend.user.repository.UserRepository;

@Service
public class TokenService {

    private static final Logger logger = LoggerFactory.getLogger(TokenService.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    private static final long REFRESH_TOKEN_TTL = 7;
    private static final TimeUnit REFRESH_TOKEN_TTL_UNIT = TimeUnit.DAYS;

    public TokenService(RedisTemplate<String, String> redisTemplate,
                                      JwtService jwtService,
                                      UserRepository userRepository) {
        this.redisTemplate = redisTemplate;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    public void storeRefreshToken(String userEmail, String token) {
        try {
            String key = RedisKeyConstants.refreshTokenKey(userEmail);
            logger.debug("Storing refresh token in Redis for: {}", userEmail);
            redisTemplate.opsForValue().set(key, token, REFRESH_TOKEN_TTL, REFRESH_TOKEN_TTL_UNIT);
            logger.debug("Successfully stored refresh token");
        } catch (Exception e) {
            logger.error("Error storing refresh token in Redis", e);
            throw new RuntimeException("Error storing refresh token in Redis: " + e.getMessage(), e);
        }
    }

    public String getRefreshToken(String userEmail) {
        try {
            String key = RedisKeyConstants.refreshTokenKey(userEmail);
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving refresh token from Redis: " + e.getMessage(), e);
        }
    }

    public boolean isTokenValid(String userEmail, String token) {
        try {
            String storedToken = getRefreshToken(userEmail);
            return storedToken != null && storedToken.equals(token);
        } catch (Exception e) {
            throw new RuntimeException("Error validating token: " + e.getMessage(), e);
        }
    }

    public void deleteRefreshToken(String userEmail) {
        try {
            String key = RedisKeyConstants.refreshTokenKey(userEmail);
            logger.debug("Attempting to delete refresh token for: {}", userEmail);
            Boolean exists = redisTemplate.hasKey(key);

            if (Boolean.TRUE.equals(exists)) {
                Boolean deleted = redisTemplate.delete(key);
                if (Boolean.TRUE.equals(deleted)) {
                    logger.debug("Token successfully removed from Redis");
                } else {
                    logger.warn("Failed to delete token from Redis");
                }
            } else {
                logger.warn("Token does not exist in Redis");
            }
        } catch (Exception e) {
            logger.error("Error deleting refresh token from Redis", e);
            throw new RuntimeException("Error deleting refresh token: " + e.getMessage(), e);
        }
    }

    @Transactional
    public AuthResult refreshAccessToken(String refreshToken) {
        logger.debug("Validating refresh token");

        if (refreshToken == null || jwtService.isTokenExpired(refreshToken)) {
            throw new InvalidRefreshTokenException("Invalid or expired refresh token");
        }

        String email = jwtService.extractEmail(refreshToken);
        logger.debug("Extracted email from refresh token: {}", email);

        // Check if refresh token exists in Redis
        if (!isTokenValid(email, refreshToken)) {
            throw new TokenRevokedException("Refresh token has been revoked. Please login again.");
        }

        logger.debug("Refresh token validated in Redis");

        // Fetch user to get role
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidRefreshTokenException("User not found"));

        // Defense in depth: Check if account is suspended
        if (!user.getStatus().name().equals("ACTIVE")) {
            logger.warn("Refresh token request from suspended user: {}", email);
            throw new AccountSuspendedException(email);
        }

        // Generate new tokens
        String newAccessToken = jwtService.generateAccessToken(email, user.getRole().name());
        String newRefreshToken = jwtService.generateRefreshToken(email, user.getRole().name());

        // Store new refresh token in Redis
        storeRefreshToken(email, newRefreshToken);
        logger.debug("New refresh token stored in Redis");

        return new AuthResult(new com.aigymtrainer.backend.auth.dto.AuthTokens(newAccessToken, newRefreshToken), user);
    }

    @Transactional
    public void logout(String refreshToken, String accessToken) {
        logger.info("Processing logout request");

        if (refreshToken != null && !refreshToken.isEmpty()) {
            try {
                String email = jwtService.extractEmail(refreshToken);
                logger.debug("Extracted email from refresh token: {}", email);
                deleteRefreshToken(email);
                logger.info("User logged out successfully: {}", email);
            } catch (Exception e) {
                logger.error("Error during logout", e);
                throw new RuntimeException("Error during logout: " + e.getMessage(), e);
            }
        }

        if (accessToken != null && !accessToken.isEmpty()) {
            try {
                // Optionally blacklist access token
                long expirationTime = jwtService.getTokenExpirationTime(accessToken);
                long ttlSeconds = (expirationTime - System.currentTimeMillis()) / 1000;
                
                if (ttlSeconds > 0) {
                    String key = RedisKeyConstants.blacklistKey(accessToken);
                    redisTemplate.opsForValue().set(key, "BLACKLISTED", ttlSeconds, TimeUnit.SECONDS);
                    logger.debug("Access token blacklisted");
                }
            } catch (Exception e) {
                logger.warn("Failed to blacklist access token: {}", e.getMessage());
            }
        }
    }
}
