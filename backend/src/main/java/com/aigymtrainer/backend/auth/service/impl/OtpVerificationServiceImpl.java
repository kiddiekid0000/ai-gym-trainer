package com.aigymtrainer.backend.auth.service.impl;

import com.aigymtrainer.backend.auth.service.OtpVerificationService;
import com.aigymtrainer.backend.common.constant.RedisKeyConstants;
import com.aigymtrainer.backend.common.exception.InvalidOtpException;
import com.aigymtrainer.backend.common.exception.OtpBlockedException;
import com.aigymtrainer.backend.common.exception.UserNotFoundException;
import com.aigymtrainer.backend.infrastructure.email.EmailService;
import com.aigymtrainer.backend.security.ratelimit.RateLimitService;
import com.aigymtrainer.backend.user.domain.User;
import com.aigymtrainer.backend.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;

@Service
public class OtpVerificationServiceImpl implements OtpVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(OtpVerificationServiceImpl.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final RateLimitService rateLimitService;
    private final SecureRandom secureRandom = new SecureRandom();

    private static final int OTP_LENGTH = 6;
    private static final int OTP_TTL_MINUTES = 10;
    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final int BLOCK_DURATION_HOURS = 1;
    private static final int MAX_RESEND_ATTEMPTS = 5;
    private static final int RESEND_WINDOW_SECONDS = 60;

    public OtpVerificationServiceImpl(RedisTemplate<String, String> redisTemplate,
                                      EmailService emailService,
                                      UserRepository userRepository,
                                      RateLimitService rateLimitService) {
        this.redisTemplate = redisTemplate;
        this.emailService = emailService;
        this.userRepository = userRepository;
        this.rateLimitService = rateLimitService;
    }

    @Override
    public void sendOtp(String email) {
        // Check if user exists
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        // Generate and store OTP
        String otp = generateOtp(email);
        logger.info("OTP generated for: {}", email);

        // Send via email service
        emailService.sendOtpEmail(email, otp);
        logger.info("OTP email sent to: {}", email);
    }

    @Override
    public void resendOtp(String email) {
        // Check rate limit for resend attempts
        if (rateLimitService.isOtpResendLimited(email)) {
            logger.warn("OTP resend limit exceeded for: {}", email);
            throw new OtpBlockedException(email);
        }

        // Generate new OTP
        sendOtp(email);
    }

    @Override
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
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
        user.setVerified(true);
        user.setStatus(com.aigymtrainer.backend.user.domain.Status.ACTIVE);
        userRepository.save(user);

        // Reset failed attempts
        resetFailedAttempts(email);
        logger.info("OTP verified successfully for: {}", email);
    }

    private String generateOtp(String email) {
        int otpValue = 100000 + secureRandom.nextInt(900000);
        String otpStr = String.valueOf(otpValue);

        String key = RedisKeyConstants.otpKey(email);
        redisTemplate.opsForValue().set(key, otpStr, Duration.ofMinutes(OTP_TTL_MINUTES));

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
        redisTemplate.opsForValue().set(key, String.valueOf(attempts), Duration.ofHours(BLOCK_DURATION_HOURS));
    }

    private void resetFailedAttempts(String email) {
        String key = RedisKeyConstants.otpFailedKey(email);
        redisTemplate.delete(key);
    }

    private boolean isBlocked(String email) {
        return getFailedAttempts(email) >= MAX_FAILED_ATTEMPTS;
    }
}
