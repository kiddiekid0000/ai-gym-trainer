package com.aigymtrainer.backend.auth.controller;

import com.aigymtrainer.backend.auth.dto.AuthResponse;
import com.aigymtrainer.backend.auth.dto.AuthResult;
import com.aigymtrainer.backend.auth.dto.LoginRequest;
import com.aigymtrainer.backend.auth.dto.OtpRequest;
import com.aigymtrainer.backend.auth.service.AuthenticationService;
import com.aigymtrainer.backend.auth.service.OtpVerificationService;
import com.aigymtrainer.backend.auth.service.RegistrationService;
import com.aigymtrainer.backend.auth.service.TokenManagementService;
import com.aigymtrainer.backend.user.dto.UserRegistrationDto;
import com.aigymtrainer.backend.user.domain.User;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationService authenticationService;
    private final RegistrationService registrationService;
    private final OtpVerificationService otpVerificationService;
    private final TokenManagementService tokenManagementService;

    @Value("${app.environment:development}")
    private String environment;

    public AuthController(AuthenticationService authenticationService,
                         RegistrationService registrationService,
                         OtpVerificationService otpVerificationService,
                         TokenManagementService tokenManagementService) {
        this.authenticationService = authenticationService;
        this.registrationService = registrationService;
        this.otpVerificationService = otpVerificationService;
        this.tokenManagementService = tokenManagementService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody UserRegistrationDto userDto, HttpServletResponse response) {
        logger.info("Register request for email: {}", userDto.getEmail());
        
        AuthResult result = registrationService.register(userDto);
        User user = result.user();

        return new AuthResponse(user.getId(), user.getEmail(), user.getRole().name(), 
                               "PENDING_OTP_VERIFICATION", "Registration successful. OTP sent to your email.");
    }

    @PostMapping("/send-otp")
    public AuthResponse sendOtp(@Valid @RequestBody OtpRequest request) {
        logger.info("Send OTP request for email: {}", request.getEmail());
        
        otpVerificationService.sendOtp(request.getEmail());
        return new AuthResponse(null, request.getEmail(), null, null, "OTP sent to your email");
    }

    @PostMapping("/verify-otp")
    public AuthResponse verifyOtp(@Valid @RequestBody OtpRequest request, HttpServletResponse response) {
        logger.info("Verify OTP request for email: {}", request.getEmail());
        
        otpVerificationService.verifyOtp(request.getEmail(), request.getOtp());
        return new AuthResponse(null, request.getEmail(), null, "VERIFIED", "Email verified successfully. You can now login.");
    }

    @PostMapping("/resend-otp")
    public AuthResponse resendOtp(@Valid @RequestBody OtpRequest request) {
        logger.info("Resend OTP request for email: {}", request.getEmail());
        
        otpVerificationService.resendOtp(request.getEmail());
        return new AuthResponse(null, request.getEmail(), null, null, "OTP resent to your email");
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        logger.info("Login request for email: {}", request.getEmail());
        
        AuthResult result = authenticationService.authenticate(request);
        User user = result.user();

        if (result.tokens() != null) {
            setAuthCookies(response, result.tokens().getAccessToken(), result.tokens().getRefreshToken());
        }

        return new AuthResponse(user.getId(), user.getEmail(), user.getRole().name(), 
                               "AUTHENTICATED", "Login successful");
    }

    @PostMapping("/logout")
    public AuthResponse logout(
            @CookieValue(required = false) String refreshToken,
            @CookieValue(required = false) String accessToken,
            HttpServletResponse response) {
        logger.info("Logout request");

        tokenManagementService.logout(refreshToken, accessToken);
        clearAuthCookies(response);

        return new AuthResponse(null, null, null, null, "Logged out successfully");
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(
            @CookieValue(required = false) String refreshToken,
            HttpServletResponse response) {
        logger.info("Refresh token request");

        AuthResult result = tokenManagementService.refreshAccessToken(refreshToken);
        User user = result.user();

        if (result.tokens() != null) {
            setAuthCookies(response, result.tokens().getAccessToken(), result.tokens().getRefreshToken());
        }

        return new AuthResponse(user.getId(), user.getEmail(), user.getRole().name(), 
                               user.getStatus().name(), "Token refreshed");
    }

    private void setAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        boolean isSecure = "production".equals(environment);
        
        ResponseCookie accessCookie = ResponseCookie
                .from("accessToken", accessToken)
                .httpOnly(true)
                .secure(isSecure)
                .path("/")
                .maxAge(15 * 60) // 15 minutes
                .build();

        ResponseCookie refreshCookie = ResponseCookie
                .from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(isSecure)
                .path("/")
                .maxAge(7 * 24 * 60 * 60) // 7 days
                .build();

        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());
    }

    private void clearAuthCookies(HttpServletResponse response) {
        ResponseCookie accessCookie = ResponseCookie
                .from("accessToken", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .build();

        ResponseCookie refreshCookie = ResponseCookie
                .from("refreshToken", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());
    }
}
