package com.aigymtrainer.backend.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aigymtrainer.backend.auth.dto.AuthResult;
import com.aigymtrainer.backend.auth.dto.AuthTokens;
import com.aigymtrainer.backend.auth.dto.LoginRequest;
import com.aigymtrainer.backend.exception.InvalidRefreshTokenException;
import com.aigymtrainer.backend.exception.TokenBlacklistException;
import com.aigymtrainer.backend.exception.TokenRevokedException;
import com.aigymtrainer.backend.exception.UserNotFoundException;
import com.aigymtrainer.backend.security.jwt.JwtService;
import com.aigymtrainer.backend.user.Role;
import com.aigymtrainer.backend.user.Status;
import com.aigymtrainer.backend.user.User;
import com.aigymtrainer.backend.user.UserRepository;
import com.aigymtrainer.backend.user.dto.UserRegistrationDto;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

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
    @Transactional
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

        // Send OTP - must succeed for registration to complete
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

            String accessToken = jwtService.generateAccessToken(adminEmail, Role.ADMIN.name());
            String refreshToken = jwtService.generateRefreshToken(adminEmail, Role.ADMIN.name());
            
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

        String accessToken = jwtService.generateAccessToken(user.getEmail(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail(), user.getRole().name());
        
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

    // GET USER BY EMAIL (avoid direct repo access in controller)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    // REFRESH TOKEN LOGIC
    // Returns AuthResult containing both tokens AND user info from a single DB call
    public AuthResult refreshAccessToken(String refreshToken) {
        logger.debug("Validating refresh token");
        
        // Check JWT expiration
        if (refreshToken == null || jwtService.isTokenExpired(refreshToken)) {
            throw new InvalidRefreshTokenException("Invalid or expired refresh token");
        }

        String email = jwtService.extractEmail(refreshToken);
        logger.debug("Extracted email from refresh token: {}", email);

        // Check if refresh token exists in Redis (user hasn't logged out)
        if (!tokenService.isTokenValid(email, refreshToken)) {
            throw new TokenRevokedException("Refresh token has been revoked. Please login again.");
        }

        logger.debug("Refresh token validated in Redis");

        // Fetch user to get role (single database hit)
        User user = getUserByEmail(email);

        // Generate new tokens
        String newAccessToken = jwtService.generateAccessToken(email, user.getRole().name());
        String newRefreshToken = jwtService.generateRefreshToken(email, user.getRole().name());

        // Store new refresh token in Redis
        tokenService.storeRefreshToken(email, newRefreshToken);
        logger.debug("New refresh token stored in Redis");

        // Return both tokens and user info in a single object
        return new AuthResult(new AuthTokens(newAccessToken, newRefreshToken), user);
    }

    // LOGOUT LOGIC - Security-first (throws exceptions if token revocation fails)
    // Ensures that after logout, attacker CANNOT use old tokens since they're revoked/blacklisted
    public void logout(String refreshToken, String accessToken) {
        logger.info("Processing logout request");

        // Revoke refresh token in Redis - REQUIRED for security
        // If this fails, exception is thrown and logout fails - tokens stay valid until Redis recovers
        if (refreshToken != null && !refreshToken.isEmpty()) {
            logger.debug("Validating token for logout request");
            String email = jwtService.extractEmail(refreshToken);
            logger.debug("Token validation successful, email: {}", email);

            logger.debug("Removing refresh token from Redis cache");
            tokenService.deleteRefreshToken(email);
            logger.debug("Refresh token successfully revoked from Redis");
        } else {
            logger.warn("Logout attempted without valid refresh token");
            throw new InvalidRefreshTokenException("Refresh token missing - logout failed");
        }

        // Blacklist access token - REQUIRED for security
        // If this fails, exception is thrown and logout fails - attacker cannot use this token
        if (accessToken != null && !accessToken.isEmpty()) {
            logger.debug("Blacklisting access token");
            long expirationTime = jwtService.getTokenExpirationTime(accessToken);
            tokenService.blacklistToken(accessToken, expirationTime);
            logger.debug("Access token successfully blacklisted");
        }

        logger.info("Logout completed successfully - tokens are revoked and blacklisted. Old tokens are now invalid.");
    }
}