package com.aigymtrainer.backend.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.aigymtrainer.backend.config.JwtService;
import com.aigymtrainer.backend.user.User;
import com.aigymtrainer.backend.user.UserRepository;
import com.aigymtrainer.backend.user.dto.UserRegistrationDto;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;


@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    private final AuthService authService;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final OtpService otpService;

    public AuthController(AuthService authService, JwtService jwtService, UserRepository userRepository, TokenService tokenService, OtpService otpService) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.otpService = otpService;
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
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("User not found"));
        // Return verified status - frontend should redirect to login
        return new AuthResponse(user.getId(), user.getEmail(), user.getRole().name(), "VERIFIED");
    }

    @PostMapping("/logout")
    public AuthResponse logout(HttpServletRequest request, HttpServletResponse response) {
        logger.info("Logout endpoint called");
        
        // Manually extract refresh token from cookies (works with CORS)
        String refreshToken = extractRefreshTokenFromCookie(request);
        String accessToken = extractAccessTokenFromCookie(request);
        
        if (refreshToken != null && !refreshToken.isEmpty()) {
            try {
                logger.debug("Validating token for logout request");
                String email = jwtService.extractEmail(refreshToken);
                logger.debug("Token validation successful");
                
                logger.debug("Removing token from Redis cache");
                tokenService.deleteRefreshToken(email);
                logger.debug("Token cache entry removed");
            } catch (Exception e) {
                logger.error("Error processing logout request", e);
            }
        } else {
            logger.warn("Logout attempted without valid refresh token");
        }

        // Blacklist the access token if it exists
        if (accessToken != null && !accessToken.isEmpty()) {
            try {
                logger.debug("Blacklisting access token");
                long expirationTime = jwtService.getTokenExpirationTime(accessToken);
                tokenService.blacklistToken(accessToken, expirationTime);
                logger.debug("Access token successfully blacklisted");
            } catch (Exception e) {
                logger.error("Error blacklisting access token", e);
                // Don't throw exception - logout should succeed even if blacklist fails
            }
        }
        
        // Clear cookies on client side
        logger.debug("Clearing authentication cookies");
        clearAuthCookies(response);
        logger.info("Logout completed successfully");
        
        return new AuthResponse(null, null, null, "LOGGED_OUT");
    }
    
    /**
     * Manually extract refresh token from cookies (CORS compatible)
     */
    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            logger.debug("No cookies found in request");
            return null;
        }
        
        for (Cookie cookie : request.getCookies()) {
            if ("refreshToken".equals(cookie.getName())) {
                logger.debug("Refresh token cookie located");
                return cookie.getValue();
            }
        }
        
        logger.debug("Refresh token not found in request cookies");
        return null;
    }

    /**
     * Manually extract access token from cookies (CORS compatible)
     */
    private String extractAccessTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            logger.debug("No cookies found in request");
            return null;
        }
        
        for (Cookie cookie : request.getCookies()) {
            if ("accessToken".equals(cookie.getName())) {
                logger.debug("Access token cookie located");
                return cookie.getValue();
            }
        }
        
        logger.debug("Access token not found in request cookies");
        return null;
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@CookieValue(value = "refreshToken", required = false) String refreshToken, 
                           HttpServletResponse response) {
    
        // check if refresh token is still valid (JWT expiration)
        if (refreshToken == null || jwtService.isTokenExpired(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }
        
        // get email from refresh token
        String email = jwtService.extractEmail(refreshToken);
        
        // Check if refresh token exists in Redis (user hasn't logged out)
        if (!tokenService.isTokenValid(email, refreshToken)) {
            throw new RuntimeException("Refresh token has been revoked. Please login again.");
        }
        
        // fetch user from database to get role for later use
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Create new token with role
        String newAccessToken = jwtService.generateAccessToken(email, user.getRole().name());
        String newRefreshToken = jwtService.generateRefreshToken(email, user.getRole().name());
        
        // Store new refresh token in Redis
        tokenService.storeRefreshToken(email, newRefreshToken);

        // Set new cookies
        Cookie accessCookie = new Cookie("accessToken", newAccessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(15 * 60); // 15 minutes for access token
        response.addCookie(accessCookie);

        Cookie newRefreshCookie = new Cookie("refreshToken", newRefreshToken);
        newRefreshCookie.setHttpOnly(true);
        newRefreshCookie.setSecure(true);
        newRefreshCookie.setPath("/");
        newRefreshCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days for refresh token
        response.addCookie(newRefreshCookie);

        // return user info from database
        return new AuthResponse(
            user.getId(),           
            user.getEmail(),        
            user.getRole().name(),
            "AUTHENTICATED"
        );
    }

    private void setAuthCookies(HttpServletResponse response, AuthTokens tokens) {
        Cookie accessCookie = new Cookie("accessToken", tokens.accessToken());
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(true); // Requires HTTPS
        accessCookie.setPath("/");
        accessCookie.setMaxAge(15 * 60); // 15 minutes
        response.addCookie(accessCookie);

        Cookie refreshCookie = new Cookie("refreshToken", tokens.refreshToken());
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
        response.addCookie(refreshCookie);
    }
    
    private void clearAuthCookies(HttpServletResponse response) {
        Cookie accessCookie = new Cookie("accessToken", null);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(0); // Expires immediately
        response.addCookie(accessCookie);

        Cookie refreshCookie = new Cookie("refreshToken", null);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0); // Expires immediately
        response.addCookie(refreshCookie);
    }
}