package com.aigymtrainer.backend.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aigymtrainer.backend.auth.dto.AuthResponse;
import com.aigymtrainer.backend.auth.dto.AuthResult;
import com.aigymtrainer.backend.auth.dto.AuthTokens;
import com.aigymtrainer.backend.auth.dto.LoginRequest;
import com.aigymtrainer.backend.auth.dto.OtpRequest;
import com.aigymtrainer.backend.user.User;
import com.aigymtrainer.backend.user.dto.UserRegistrationDto;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;


@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    private final AuthService authService;

    @Value("${app.environment:development}")
    private String environment;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody UserRegistrationDto userDto, HttpServletResponse response) { 
        AuthResult result = authService.register(userDto);
        // No tokens for unverified user - status indicates OTP verification is pending
        return new AuthResponse(result.user().getId(), result.user().getEmail(), result.user().getRole().name(), "PENDING_OTP_VERIFICATION");
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthResult result = authService.login(request);
        setAuthCookies(response, result.tokens());
        return new AuthResponse(result.user().getId(), result.user().getEmail(), result.user().getRole().name(), "AUTHENTICATED");
    }

    @PostMapping("/send-otp")
    public AuthResponse sendOtp(@RequestBody OtpRequest request) {
        authService.sendOtp(request.email());
        return new AuthResponse(null, request.email(), null, "OTP_SENT");
    }

    @PostMapping("/verify-otp")
    public AuthResponse verifyOtp(@Valid @RequestBody OtpRequest request, HttpServletResponse response) {
        authService.verifyOtp(request.email(), request.otp());
        // After verification, allow login
        User user = authService.getUserByEmail(request.email());
        // Return verified status - frontend should redirect to login
        return new AuthResponse(user.getId(), user.getEmail(), user.getRole().name(), "VERIFIED");
    }

    @PostMapping("/logout")
    public AuthResponse logout(
            @CookieValue(required = false) String refreshToken,
            @CookieValue(required = false) String accessToken,
            HttpServletResponse response) {
        logger.info("Logout endpoint called");

        authService.logout(refreshToken, accessToken);

        clearAuthCookies(response);
        logger.info("Logout completed successfully");

        return new AuthResponse(null, null, null, "LOGGED_OUT");
    }
    
    @PostMapping("/refresh")
    public AuthResponse refresh(
            @CookieValue(required = false) String refreshToken,
            HttpServletResponse response) {

        // Get both tokens and user info from a single database call
        AuthResult result = authService.refreshAccessToken(refreshToken);

        // Set new cookies
        setAuthCookies(response, result.tokens());

        return new AuthResponse(
            result.user().getId(),
            result.user().getEmail(),
            result.user().getRole().name(),
            "AUTHENTICATED"
        );
    }

    private void setAuthCookies(HttpServletResponse response, AuthTokens tokens) {
        boolean isProduction = "production".equalsIgnoreCase(environment);
        String sameSite = isProduction ? "None" : "Lax";
        boolean secure = isProduction;

        // Configure the Access Token Cookie
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", tokens.accessToken())
                .httpOnly(true)     // Prevents XSS (JS cannot read the token)
                .secure(secure)     // HTTPS for production, HTTP for dev
                .path("/")
                .maxAge(15 * 60)    // 15 minutes
                .sameSite(sameSite) // None for cross-site (production), Lax for same-site (dev)
                .build();

        // Configure the Refresh Token Cookie
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", tokens.refreshToken())
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(7 * 24 * 60 * 60) // 7 days
                .sameSite(sameSite)
                .build();

        // Add to response headers
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }
    
    private void clearAuthCookies(HttpServletResponse response) {
        boolean isProduction = "production".equalsIgnoreCase(environment);
        String sameSite = isProduction ? "None" : "Lax";
        boolean secure = isProduction;

        // Clear access token
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(0)          // Expires immediately
                .sameSite(sameSite)
                .build();

        // Clear refresh token
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(0)
                .sameSite(sameSite)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }
}