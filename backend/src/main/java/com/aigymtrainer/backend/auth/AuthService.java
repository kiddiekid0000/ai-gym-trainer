package com.aigymtrainer.backend.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.aigymtrainer.backend.auth.dto.AuthResponse;
import com.aigymtrainer.backend.auth.dto.AuthResult;
import com.aigymtrainer.backend.auth.dto.AuthTokens;
import com.aigymtrainer.backend.auth.dto.LoginRequest;
import com.aigymtrainer.backend.config.JwtService;
import com.aigymtrainer.backend.user.Role;
import com.aigymtrainer.backend.user.User;
import com.aigymtrainer.backend.user.UserRepository;
import com.aigymtrainer.backend.user.dto.UserRegistrationDto;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password}")
    private String adminPassword;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    // 🟢 REGISTER
    public AuthResult register(UserRegistrationDto userDto) {
        User user = new User();
        user.setEmail(userDto.getEmail());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        user.setRole(Role.USER); // Default role for registered users

        User savedUser = userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(savedUser.getEmail());
        String refreshToken = jwtService.generateRefreshToken(savedUser.getEmail());

        return new AuthResult(new AuthTokens(accessToken, refreshToken), savedUser);
    }

    // 🔵 LOGIN
    public AuthResult login(LoginRequest request) {
        // Check if it's admin login
        if (request.getEmail().equals(adminEmail) && request.getPassword().equals(adminPassword)) {
            // Admin login
            User adminUser = new User();
            adminUser.setEmail(adminEmail);
            adminUser.setRole(Role.ADMIN);
            // No ID for admin

            String accessToken = jwtService.generateAccessToken(adminEmail);
            String refreshToken = jwtService.generateRefreshToken(adminEmail);
            return new AuthResult(new AuthTokens(accessToken, refreshToken), adminUser);
        }

        // Regular user login
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        // Ensure role is set for existing users
        if (user.getRole() == null) {
            user.setRole(Role.USER);
            userRepository.save(user);
        }

        String accessToken = jwtService.generateAccessToken(user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());

        return new AuthResult(new AuthTokens(accessToken, refreshToken), user);
    }
}