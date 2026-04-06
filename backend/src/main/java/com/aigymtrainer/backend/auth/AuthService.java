package com.aigymtrainer.backend.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.aigymtrainer.backend.auth.dto.AuthResult;
import com.aigymtrainer.backend.auth.dto.AuthTokens;
import com.aigymtrainer.backend.auth.dto.LoginRequest;
import com.aigymtrainer.backend.config.JwtService;
import com.aigymtrainer.backend.user.Role;
import com.aigymtrainer.backend.user.Status;
import com.aigymtrainer.backend.user.User;
import com.aigymtrainer.backend.user.UserRepository;
import com.aigymtrainer.backend.user.dto.UserRegistrationDto;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenService tokenService;
    private final OtpService otpService;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password}")
    private String adminPassword;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       TokenService tokenService,
                       OtpService otpService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenService = tokenService;
        this.otpService = otpService;
    }

    // REGISTER
    public AuthResult register(UserRegistrationDto userDto) {

        if (userRepository.existsByEmail(userDto.getEmail())) {
        throw new RuntimeException("Email already exists"); 
    }

        User user = new User();
        user.setEmail(userDto.getEmail());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        user.setRole(Role.USER); // Default role for registered users
        user.setVerified(false); // Not verified yet

        User savedUser = userRepository.save(user);

        // Send OTP
        otpService.generateOtp(savedUser.getEmail());

        // Do not generate tokens yet, user needs to verify OTP first
        return new AuthResult(null, savedUser);
    }

    // LOGIN
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
            
            // Store refresh token in Redis
            tokenService.storeRefreshToken(adminEmail, refreshToken);
            
            return new AuthResult(new AuthTokens(accessToken, refreshToken), adminUser);
        }

        // Regular user login
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        if (!user.isVerified()) {
            throw new RuntimeException("Account not verified. Please verify your email with OTP first.");
        }

        if (user.getStatus() == Status.SUSPENDED) {
            throw new RuntimeException("Account is suspended.");
        }

        // Ensure role is set for existing users
        if (user.getRole() == null) {
            user.setRole(Role.USER);
            userRepository.save(user);
        }

        String accessToken = jwtService.generateAccessToken(user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());
        
        // Store refresh token in Redis
        tokenService.storeRefreshToken(user.getEmail(), refreshToken);

        return new AuthResult(new AuthTokens(accessToken, refreshToken), user);
    }

    public void verifyOtp(String email, String otp) {
        if (otpService.isBlocked(email)) {
            throw new RuntimeException("Too many failed OTP attempts. Try again later.");
        }

        if (!otpService.verifyOtp(email, otp)) {
            otpService.incrementFailedAttempts(email);
            throw new RuntimeException("Invalid OTP");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setVerified(true);
        userRepository.save(user);
        otpService.resetFailedAttempts(email);
    }

    public void sendOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        otpService.generateOtp(email);
    }
}