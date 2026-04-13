package com.aigymtrainer.backend.auth.service;

import com.aigymtrainer.backend.auth.dto.AuthResult;
import com.aigymtrainer.backend.exception.DuplicateEmailException;
import com.aigymtrainer.backend.user.domain.Role;
import com.aigymtrainer.backend.user.domain.Status;
import com.aigymtrainer.backend.user.domain.User;
import com.aigymtrainer.backend.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.aigymtrainer.backend.user.dto.UserRegistrationDto;

@Service
public class RegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpVerificationService otpVerificationService;

    public RegistrationService(UserRepository userRepository,
                                   PasswordEncoder passwordEncoder,
                                   OtpVerificationService otpVerificationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpVerificationService = otpVerificationService;
    }

    @Transactional
    public AuthResult register(UserRegistrationDto userDto) {
        // Check if email already exists
        if (userRepository.existsByEmail(userDto.email())) {
            throw new DuplicateEmailException(userDto.email());
        }

        // Create new user with PENDING status (awaiting OTP verification)
        User user = new User();
        user.setEmail(userDto.email());
        user.setPassword(passwordEncoder.encode(userDto.password()));
        user.setRole(Role.USER);
        user.setStatus(Status.PENDING); // PENDING until OTP is verified
        user.setVerified(false);

        // Save user to database (atomic)
        User savedUser = userRepository.save(user);
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
