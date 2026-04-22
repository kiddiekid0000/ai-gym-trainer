package com.aigymtrainer.backend.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aigymtrainer.backend.auth.dto.AuthResult;
import com.aigymtrainer.backend.user.domain.User;
import com.aigymtrainer.backend.user.dto.UserRegistrationDto;
import com.aigymtrainer.backend.user.service.UserService;

@Service
public class RegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationService.class);

    private final UserService userService;
    private final OtpVerificationService otpVerificationService;

    public RegistrationService(UserService userService,
                                   OtpVerificationService otpVerificationService) {
        this.userService = userService;
        this.otpVerificationService = otpVerificationService;
    }

    @Transactional
    public AuthResult register(UserRegistrationDto userDto) {
        // Delegate user creation to UserService
        User savedUser = userService.registerNewUser(userDto);
        logger.info("User registered with email: {}, Status: PENDING", savedUser.getEmail());

        // Try to send OTP, but don't fail registration if email fails
        try {
            otpVerificationService.sendOtp(savedUser.getEmail());
            logger.info("OTP sent successfully to: {}", savedUser.getEmail());
        } catch (Exception e) {
            logger.warn("Failed to send OTP email to {}: {}", savedUser.getEmail(), e.getMessage());
            // IMPORTANT: Do NOT rethrow exception. User is already saved in DB.
            // Return response indicating email failed but user was created
        }

        // Return AuthResult with no tokens (user not verified yet)
        return new AuthResult(null, savedUser);
    }
}
