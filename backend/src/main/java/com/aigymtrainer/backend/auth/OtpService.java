package com.aigymtrainer.backend.auth;

import java.security.SecureRandom;
import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.aigymtrainer.backend.config.EmailService;

@Service
public class OtpService {

    private final RedisTemplate<String, String> redisTemplate;
    private final EmailService emailService;

    public OtpService(RedisTemplate<String, String> redisTemplate, EmailService emailService) {
        this.redisTemplate = redisTemplate;
        this.emailService = emailService;
    }

    public String generateOtp(String email) {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000); // 6-digit OTP
        String otpStr = String.valueOf(otp);

        String key = "otp:" + email;
        redisTemplate.opsForValue().set(key, otpStr, Duration.ofMinutes(10));

        emailService.sendOtpEmail(email, otpStr);
        return otpStr;
    }

    public boolean verifyOtp(String email, String otp) {
        String key = "otp:" + email;
        String storedOtp = redisTemplate.opsForValue().get(key);
        if (storedOtp != null && storedOtp.equals(otp)) {
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }

    public int getFailedAttempts(String email) {
        String key = "otp_failed:" + email;
        String attempts = redisTemplate.opsForValue().get(key);
        return attempts != null ? Integer.parseInt(attempts) : 0;
    }

    public void incrementFailedAttempts(String email) {
        String key = "otp_failed:" + email;
        int attempts = getFailedAttempts(email) + 1;
        redisTemplate.opsForValue().set(key, String.valueOf(attempts), Duration.ofHours(1)); // Reset after 1 hour
    }

    public void resetFailedAttempts(String email) {
        String key = "otp_failed:" + email;
        redisTemplate.delete(key);
    }

    public boolean isBlocked(String email) {
        return getFailedAttempts(email) >= 3;
    }
}