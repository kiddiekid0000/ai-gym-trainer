package com.aigymtrainer.backend.auth.service;

import java.security.SecureRandom;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aigymtrainer.backend.common.constant.AuthConstants;
import com.aigymtrainer.backend.common.constant.RedisKeyConstants;
import com.aigymtrainer.backend.exception.InvalidOtpException;
import com.aigymtrainer.backend.exception.OtpBlockedException;
import com.aigymtrainer.backend.infrastructure.email.EmailService;
import com.aigymtrainer.backend.security.service.RateLimitService;
import com.aigymtrainer.backend.user.domain.User;
import com.aigymtrainer.backend.user.service.UserService;

@Service
public class OtpVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(OtpVerificationService.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final EmailService emailService;
    private final UserService userService;
    private final RateLimitService rateLimitService;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpVerificationService(RedisTemplate<String, String> redisTemplate,
                                      EmailService emailService,
                                      UserService userService,
                                      RateLimitService rateLimitService) {
        this.redisTemplate = redisTemplate;
        this.emailService = emailService;
        this.userService = userService;
        this.rateLimitService = rateLimitService;
    }

    public void sendOtp(String email) {
        // Check if user exists
        User user = userService.findByEmail(email);

        // Generate and store OTP
        String otp = generateOtp(email);
        logger.info("OTP generated for: {}", email);

        // Send via email service
        emailService.sendOtpEmail(email, otp);
        logger.info("OTP email sent to: {}", email);
    }

    public void resendOtp(String email) {
        // Check rate limit for resend attempts
        if (rateLimitService.isOtpResendLimited(email)) {
            logger.warn("OTP resend limit exceeded for: {}", email);
            throw new OtpBlockedException(email);
        }

        // Generate new OTP
        sendOtp(email);
    }

    @Transactional
    public void verifyOtp(String email, String otp) {
        // Check if user is blocked due to too many failed attempts
        if (isBlocked(email)) {
            logger.warn("Too many failed OTP attempts for: {}", email);
            throw new OtpBlockedException(email);
        }

        // Verify OTP
        if (!verifyOtpInternal(email, otp)) {
            incrementFailedAttempts(email);
            logger.warn("Invalid OTP attempt for: {}", email);
            throw new InvalidOtpException();
        }

        // Mark user as verified and ACTIVE
        User user = userService.findByEmail(email);
        user.setVerified(true);
        user.setStatus(com.aigymtrainer.backend.user.domain.Status.ACTIVE);
        userService.save(user);

        // Reset failed attempts
        resetFailedAttempts(email);
        logger.info("OTP verified successfully for: {}", email);
    }

    private String generateOtp(String email) {
        int otpValue = 100000 + secureRandom.nextInt(900000);
        String otpStr = String.valueOf(otpValue);

        String key = RedisKeyConstants.otpKey(email);
        redisTemplate.opsForValue().set(key, otpStr, Duration.ofMinutes(AuthConstants.OTP_TTL_MINUTES));

        return otpStr;
    }

    private boolean verifyOtpInternal(String email, String otp) {
        String key = RedisKeyConstants.otpKey(email);
        String storedOtp = redisTemplate.opsForValue().get(key);

        if (storedOtp != null && storedOtp.equals(otp)) {
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }

    private int getFailedAttempts(String email) {
        String key = RedisKeyConstants.otpFailedKey(email);
        String attempts = redisTemplate.opsForValue().get(key);
        return attempts != null ? Integer.parseInt(attempts) : 0;
    }

    private void incrementFailedAttempts(String email) {
        String key = RedisKeyConstants.otpFailedKey(email);
        int attempts = getFailedAttempts(email) + 1;
        redisTemplate.opsForValue().set(key, String.valueOf(attempts), Duration.ofHours(AuthConstants.OTP_BLOCK_DURATION_HOURS));
    }

    private void resetFailedAttempts(String email) {
        String key = RedisKeyConstants.otpFailedKey(email);
        redisTemplate.delete(key);
    }

    private boolean isBlocked(String email) {
        return getFailedAttempts(email) >= AuthConstants.OTP_MAX_FAILED_ATTEMPTS;
    }
}
