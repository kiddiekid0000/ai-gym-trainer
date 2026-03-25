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

    public AuthController(AuthService authService, JwtService jwtService, UserRepository userRepository, TokenService tokenService) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody UserRegistrationDto userDto, HttpServletResponse response) { 
        AuthResult result = authService.register(userDto);
        setAuthCookies(response, result.tokens());
        return new AuthResponse(result.user().getId(), result.user().getEmail(), result.user().getRole().name());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthResult result = authService.login(request);
        setAuthCookies(response, result.tokens());
        return new AuthResponse(result.user().getId(), result.user().getEmail(), result.user().getRole().name());
    }

    @PostMapping("/logout")
    public AuthResponse logout(HttpServletRequest request, HttpServletResponse response) {
        logger.info("Logout endpoint called");
        
        // Manually extract refresh token from cookies (works with CORS)
        String refreshToken = extractRefreshTokenFromCookie(request);
        
        if (refreshToken != null && !refreshToken.isEmpty()) {
            try {
                logger.info("Attempting to extract email from refresh token");
                String email = jwtService.extractEmail(refreshToken);
                logger.info("Email extracted: {}", email);
                
                logger.info("Deleting refresh token from Redis for email: {}", email);
                tokenService.deleteRefreshToken(email);
                logger.info("Token successfully deleted from Redis for email: {}", email);
            } catch (Exception e) {
                logger.error("Error extracting email or deleting from Redis", e);
            }
        } else {
            logger.warn("Refresh token not found in cookies");
        }
        
        // Clear cookies on client side
        logger.info("Clearing authentication cookies");
        clearAuthCookies(response);
        logger.info("Logout completed successfully");
        
        return new AuthResponse(null, null, null);
    }
    
    /**
     * Manually extract refresh token from cookies (CORS compatible)
     */
    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            logger.warn("No cookies found in request");
            return null;
        }
        
        for (Cookie cookie : request.getCookies()) {
            if ("refreshToken".equals(cookie.getName())) {
                logger.debug("Refresh token found in cookies");
                return cookie.getValue();
            }
        }
        
        logger.warn("Refresh token cookie not found. Available cookies: {}", 
            java.util.Arrays.stream(request.getCookies())
                .map(Cookie::getName)
                .toList());
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
        
        // Create new token
        String newAccessToken = jwtService.generateAccessToken(email);
        String newRefreshToken = jwtService.generateRefreshToken(email);
        
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
            user.getRole().name()   
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