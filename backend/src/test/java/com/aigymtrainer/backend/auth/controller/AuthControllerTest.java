package com.aigymtrainer.backend.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.aigymtrainer.backend.auth.dto.AuthResult;
import com.aigymtrainer.backend.auth.dto.AuthTokens;
import com.aigymtrainer.backend.auth.service.AuthenticationService;
import com.aigymtrainer.backend.auth.service.OtpVerificationService;
import com.aigymtrainer.backend.auth.service.RegistrationService;
import com.aigymtrainer.backend.auth.service.TokenService;
import com.aigymtrainer.backend.user.domain.Role;
import com.aigymtrainer.backend.user.domain.Status;
import com.aigymtrainer.backend.user.domain.User;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private RegistrationService registrationService;

    @Mock
    private OtpVerificationService otpVerificationService;

    @Mock
    private TokenService tokenService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void register_shouldReturnSuccess_whenValidRegistration() throws Exception {
        // Given
        User user = createTestUser(1L, "test@example.com", Status.PENDING);
        AuthResult authResult = new AuthResult(null, user);

        given(registrationService.register(any())).willReturn(authResult);

        // When & Then
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email": "test@example.com",
                        "password": "Password123!"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(1))
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.role").value("USER"))
            .andExpect(jsonPath("$.status").value("PENDING_OTP_VERIFICATION"))
            .andExpect(jsonPath("$.message").value("Registration successful. OTP sent to your email."));

        verify(registrationService).register(any());
    }

    @Test
    void login_shouldReturnSuccess_whenValidCredentials() throws Exception {
        // Given
        User user = createTestUser(1L, "user@example.com", Status.ACTIVE);
        AuthTokens tokens = new AuthTokens("access.token", "refresh.token");
        AuthResult authResult = new AuthResult(tokens, user);

        given(authenticationService.authenticate(any())).willReturn(authResult);

        // When & Then
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email": "user@example.com",
                        "password": "Password123!"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(1))
            .andExpect(jsonPath("$.email").value("user@example.com"))
            .andExpect(jsonPath("$.role").value("USER"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.message").value("Login successful"));

        verify(authenticationService).authenticate(any());
    }

    @Test
    void sendOtp_shouldReturnSuccess() throws Exception {
        // When & Then
        mockMvc.perform(post("/auth/send-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email": "test@example.com"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.message").value("OTP sent to your email"));

        verify(otpVerificationService).sendOtp("test@example.com");
    }

    @Test
    void verifyOtp_shouldReturnSuccess() throws Exception {
        // When & Then
        mockMvc.perform(post("/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email": "test@example.com",
                        "otp": "123456"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.status").value("VERIFIED"))
            .andExpect(jsonPath("$.message").value("Email verified successfully. You can now login."));

        verify(otpVerificationService).verifyOtp("test@example.com", "123456");
    }

    @Test
    void resendOtp_shouldReturnSuccess() throws Exception {
        // When & Then
        mockMvc.perform(post("/auth/resend-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email": "test@example.com"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.message").value("OTP resent to your email"));

        verify(otpVerificationService).resendOtp("test@example.com");
    }

    @Test
    void logout_shouldReturnSuccess() throws Exception {
        // When & Then
        mockMvc.perform(post("/auth/logout"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Logged out successfully"));

        verify(tokenService).logout(null, null);
    }

    @Test
    void refresh_shouldReturnSuccess_whenValidRefreshToken() throws Exception {
        // Given
        User user = createTestUser(1L, "user@example.com", Status.ACTIVE);
        AuthTokens tokens = new AuthTokens("new.access.token", "new.refresh.token");
        AuthResult authResult = new AuthResult(tokens, user);

        given(tokenService.refreshAccessToken("refresh.token")).willReturn(authResult);

        // When & Then
        mockMvc.perform(post("/auth/refresh")
                .cookie(new jakarta.servlet.http.Cookie("refreshToken", "refresh.token")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(1))
            .andExpect(jsonPath("$.email").value("user@example.com"))
            .andExpect(jsonPath("$.role").value("USER"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.message").value("Token refreshed"));

        verify(tokenService).refreshAccessToken("refresh.token");
    }

    private User createTestUser(Long id, String email, Status status) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPassword("password");
        user.setRole(Role.USER);
        user.setStatus(status);
        user.setVerified(status == Status.ACTIVE);
        return user;
    }
}