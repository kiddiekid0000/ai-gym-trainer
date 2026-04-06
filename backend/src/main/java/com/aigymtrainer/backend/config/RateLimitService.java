package com.aigymtrainer.backend.config;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RateLimitService {

    private final RedisTemplate<String, String> redisTemplate;

    public RateLimitService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isAllowed(String key, int maxRequests, Duration window) {
        String countKey = "rate_limit:" + key;
        String windowKey = "rate_limit_window:" + key;

        Long currentCount = redisTemplate.opsForValue().increment(countKey);
        if (currentCount == 1) {
            // First request, set expiration
            redisTemplate.expire(countKey, window);
            redisTemplate.opsForValue().set(windowKey, String.valueOf(System.currentTimeMillis()));
            redisTemplate.expire(windowKey, window);
        }

        return currentCount <= maxRequests;
    }

    public void reset(String key) {
        String countKey = "rate_limit:" + key;
        String windowKey = "rate_limit_window:" + key;
        redisTemplate.delete(countKey);
        redisTemplate.delete(windowKey);
    }
}