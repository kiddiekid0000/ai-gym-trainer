package com.aigymtrainer.backend.auth;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private static final Logger logger = LoggerFactory.getLogger(TokenService.class);
    
    private final RedisTemplate<String, String> redisTemplate;
    
    private static final long REFRESH_TOKEN_TTL = 7; // 7 days
    private static final TimeUnit REFRESH_TOKEN_TTL_UNIT = TimeUnit.DAYS;
    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    public TokenService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Store refresh token in Redis with TTL of 7 days
     * 
     * @param userEmail the user's email (acts as identifier)
     * @param token the refresh token to store
     */
    public void storeRefreshToken(String userEmail, String token) {
        try {
            String key = REFRESH_TOKEN_PREFIX + userEmail;
            logger.info("Storing refresh token in Redis for user: {}", userEmail);
            redisTemplate.opsForValue().set(key, token, REFRESH_TOKEN_TTL, REFRESH_TOKEN_TTL_UNIT);
            logger.info("Successfully stored refresh token for user: {} with TTL: {} {}", 
                userEmail, REFRESH_TOKEN_TTL, REFRESH_TOKEN_TTL_UNIT);
        } catch (Exception e) {
            logger.error("Error storing refresh token in Redis for user: {}", userEmail, e);
            throw new RuntimeException("Error storing refresh token in Redis: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieve refresh token from Redis
     * 
     * @param userEmail the user's email
     * @return the refresh token if it exists, null otherwise
     */
    public String getRefreshToken(String userEmail) {
        try {
            String key = REFRESH_TOKEN_PREFIX + userEmail;
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving refresh token from Redis: " + e.getMessage(), e);
        }
    }

    /**
     * Check if a refresh token exists and is valid in Redis
     * 
     * @param userEmail the user's email
     * @param token the token to check
     * @return true if the token exists and matches, false otherwise
     */
    public boolean isTokenValid(String userEmail, String token) {
        try {
            String storedToken = getRefreshToken(userEmail);
            return storedToken != null && storedToken.equals(token);
        } catch (Exception e) {
            throw new RuntimeException("Error validating token: " + e.getMessage(), e);
        }
    }

    /**
     * Delete refresh token from Redis (logout)
     * 
     * @param userEmail the user's email
     */
    public void deleteRefreshToken(String userEmail) {
        try {
            String key = REFRESH_TOKEN_PREFIX + userEmail;
            logger.info("Attempting to delete token from Redis with key: {}", key);
            
            // Check if key exists before deletion
            Boolean exists = redisTemplate.hasKey(key);
            logger.info("Token exists in Redis before deletion: {}", exists);
            
            if (Boolean.TRUE.equals(exists)) {
                Boolean deleted = redisTemplate.delete(key);
                logger.info("Token deletion result: {}", deleted);
                
                if (Boolean.TRUE.equals(deleted)) {
                    logger.info("Successfully deleted refresh token for user: {}", userEmail);
                    
                    // Verify deletion
                    Boolean stillExists = redisTemplate.hasKey(key);
                    logger.info("Token still exists after deletion: {}", stillExists);
                } else {
                    logger.warn("Failed to delete token for user: {}", userEmail);
                }
            } else {
                logger.warn("Token does not exist in Redis for user: {}", userEmail);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error deleting refresh token from Redis: " + e.getMessage(), e);
        }
    }

    /**
     * Check if a refresh token exists in Redis
     * 
     * @param userEmail the user's email
     * @return true if the token exists, false otherwise
     */
    public boolean tokenExists(String userEmail) {
        try {
            String key = REFRESH_TOKEN_PREFIX + userEmail;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            throw new RuntimeException("Error checking token existence: " + e.getMessage(), e);
        }
    }
}
