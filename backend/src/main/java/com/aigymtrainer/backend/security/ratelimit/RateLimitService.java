package com.aigymtrainer.backend.security.ratelimit;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final long RATE_LIMIT_WINDOW = 60; // seconds
    private static final int IP_RATE_LIMIT = 10; // requests per minute per IP
    private static final int USER_RATE_LIMIT = 20; // requests per minute per user

    public RateLimitService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // Check IP-based rate limit (for /auth endpoints)
    public boolean isIpRateLimited(String ip) {
        String key = "rate_limit:ip:" + ip;
        Long count = redisTemplate.opsForValue().increment(key);
        
        if (count == 1) {
            redisTemplate.expire(key, RATE_LIMIT_WINDOW, TimeUnit.SECONDS);
        }
        
        return count > IP_RATE_LIMIT;
    }

    // Check user-based rate limit
    public boolean isUserRateLimited(String email) {
        String key = "rate_limit:user:" + email;
        Long count = redisTemplate.opsForValue().increment(key);
        
        if (count == 1) {
            redisTemplate.expire(key, RATE_LIMIT_WINDOW, TimeUnit.SECONDS);
        }
        
        return count > USER_RATE_LIMIT;
    }

    // Get remaining requests for IP
    public long getIpRemainingRequests(String ip) {
        String key = "rate_limit:ip:" + ip;
        String count = redisTemplate.opsForValue().get(key);
        return Math.max(0, IP_RATE_LIMIT - (count != null ? Long.parseLong(count) : 0));
    }

    // Get remaining requests for user
    public long getUserRemainingRequests(String email) {
        String key = "rate_limit:user:" + email;
        String count = redisTemplate.opsForValue().get(key);
        return Math.max(0, USER_RATE_LIMIT - (count != null ? Long.parseLong(count) : 0));
    }
}
