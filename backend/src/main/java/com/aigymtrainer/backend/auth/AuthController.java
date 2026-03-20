package com.aigymtrainer.backend.auth;

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
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;


@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final UserRepository userRepository;  


    public AuthController(AuthService authService, JwtService jwtService, UserRepository userRepository) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
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

    @PostMapping("/refresh")
    public AuthResponse refresh(@CookieValue(value = "refreshToken", required = false) String refreshToken, 
                           HttpServletResponse response) {
    
    // check if refresh token is still valid
    if (refreshToken == null || jwtService.isTokenExpired(refreshToken)) {
        throw new RuntimeException("Invalid refresh token");
    }
    
    // get email from refresh token
    String email = jwtService.extractEmail(refreshToken);
    
    // fetch user from database to get role for later use
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new RuntimeException("User not found"));
    
    // Create new token
    String newAccessToken = jwtService.generateAccessToken(email);
    String newRefreshToken = jwtService.generateRefreshToken(email);

    // 4. Set new cookies
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

    // return user infor from database
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
}