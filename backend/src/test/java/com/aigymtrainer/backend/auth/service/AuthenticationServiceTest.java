package com.aigymtrainer.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.aigymtrainer.backend.auth.dto.AuthResult;
import com.aigymtrainer.backend.auth.dto.LoginRequest;
import com.aigymtrainer.backend.exception.AccountNotVerifiedException;
import com.aigymtrainer.backend.exception.AccountSuspendedException;
import com.aigymtrainer.backend.exception.InvalidCredentialsException;
import com.aigymtrainer.backend.security.service.JwtService;
import com.aigymtrainer.backend.user.domain.Role;
import com.aigymtrainer.backend.user.domain.Status;
import com.aigymtrainer.backend.user.domain.User;
import com.aigymtrainer.backend.user.service.UserService;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void authenticate_shouldAuthenticateAdmin_whenValidAdminCredentials() {
        // Given
        ReflectionTestUtils.setField(authenticationService, "adminEmail", "admin@example.com");
        ReflectionTestUtils.setField(authenticationService, "adminPassword", "adminPass123");

        LoginRequest request = new LoginRequest("admin@example.com", "adminPass123");
        String accessToken = "admin.access.token";
        String refreshToken = "admin.refresh.token";

        given(jwtService.generateAccessToken("admin@example.com", "ADMIN")).willReturn(accessToken);
        given(jwtService.generateRefreshToken("admin@example.com", "ADMIN")).willReturn(refreshToken);

        // When
        AuthResult result = authenticationService.authenticate(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.user()).isNotNull();
        assertThat(result.user().getEmail()).isEqualTo("admin@example.com");
        assertThat(result.user().getRole()).isEqualTo(Role.ADMIN);
        assertThat(result.user().getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(result.user().isVerified()).isTrue();
        assertThat(result.tokens()).isNotNull();
        assertThat(result.tokens().accessToken()).isEqualTo(accessToken);
        assertThat(result.tokens().refreshToken()).isEqualTo(refreshToken);

        verify(jwtService).generateAccessToken("admin@example.com", "ADMIN");
        verify(jwtService).generateRefreshToken("admin@example.com", "ADMIN");
        verify(tokenService).storeRefreshToken("admin@example.com", refreshToken);
        verify(userService, never()).findByEmail(anyString());
    }

    @Test
    void authenticate_shouldThrowException_whenInvalidAdminPassword() {
        // Given
        ReflectionTestUtils.setField(authenticationService, "adminEmail", "admin@example.com");
        ReflectionTestUtils.setField(authenticationService, "adminPassword", "adminPass123");

        LoginRequest request = new LoginRequest("admin@example.com", "wrongPassword");

        // When & Then
        assertThatThrownBy(() -> authenticationService.authenticate(request))
            .isInstanceOf(InvalidCredentialsException.class)
            .hasMessageContaining("Invalid admin credentials");

        verify(jwtService, never()).generateAccessToken(anyString(), anyString());
        verify(jwtService, never()).generateRefreshToken(anyString(), anyString());
        verify(tokenService, never()).storeRefreshToken(anyString(), anyString());
    }

    @Test
    void authenticate_shouldAuthenticateUser_whenValidCredentialsAndVerified() {
        // Given
        LoginRequest request = new LoginRequest("user@example.com", "password123");
        User user = createTestUser("user@example.com", "encodedPassword", Status.ACTIVE, true);
        String accessToken = "user.access.token";
        String refreshToken = "user.refresh.token";

        given(userService.findByEmail("user@example.com")).willReturn(user);
        given(passwordEncoder.matches("password123", "encodedPassword")).willReturn(true);
        given(jwtService.generateAccessToken("user@example.com", "USER")).willReturn(accessToken);
        given(jwtService.generateRefreshToken("user@example.com", "USER")).willReturn(refreshToken);

        // When
        AuthResult result = authenticationService.authenticate(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.user()).isEqualTo(user);
        assertThat(result.tokens()).isNotNull();
        assertThat(result.tokens().accessToken()).isEqualTo(accessToken);
        assertThat(result.tokens().refreshToken()).isEqualTo(refreshToken);

        verify(userService).findByEmail("user@example.com");
        verify(passwordEncoder).matches("password123", "encodedPassword");
        verify(jwtService).generateAccessToken("user@example.com", "USER");
        verify(jwtService).generateRefreshToken("user@example.com", "USER");
        verify(tokenService).storeRefreshToken("user@example.com", refreshToken);
    }

    @Test
    void authenticate_shouldThrowException_whenUserNotVerified() {
        // Given
        LoginRequest request = new LoginRequest("user@example.com", "password123");
        User user = createTestUser("user@example.com", "encodedPassword", Status.PENDING, false);

        given(userService.findByEmail("user@example.com")).willReturn(user);
        given(passwordEncoder.matches("password123", "encodedPassword")).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> authenticationService.authenticate(request))
            .isInstanceOf(AccountNotVerifiedException.class)
            .hasMessageContaining("user@example.com");

        verify(userService).findByEmail("user@example.com");
        verify(passwordEncoder).matches("password123", "encodedPassword");
        verify(jwtService, never()).generateAccessToken(anyString(), anyString());
        verify(jwtService, never()).generateRefreshToken(anyString(), anyString());
    }

    @Test
    void authenticate_shouldThrowException_whenUserSuspended() {
        // Given
        LoginRequest request = new LoginRequest("user@example.com", "password123");
        User user = createTestUser("user@example.com", "encodedPassword", Status.SUSPENDED, true);

        given(userService.findByEmail("user@example.com")).willReturn(user);
        given(passwordEncoder.matches("password123", "encodedPassword")).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> authenticationService.authenticate(request))
            .isInstanceOf(AccountSuspendedException.class)
            .hasMessageContaining("user@example.com");

        verify(userService).findByEmail("user@example.com");
        verify(passwordEncoder).matches("password123", "encodedPassword");
        verify(jwtService, never()).generateAccessToken(anyString(), anyString());
        verify(jwtService, never()).generateRefreshToken(anyString(), anyString());
    }

    @Test
    void authenticate_shouldThrowException_whenInvalidPassword() {
        // Given
        LoginRequest request = new LoginRequest("user@example.com", "wrongPassword");
        User user = createTestUser("user@example.com", "encodedPassword", Status.ACTIVE, true);

        given(userService.findByEmail("user@example.com")).willReturn(user);
        given(passwordEncoder.matches("wrongPassword", "encodedPassword")).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> authenticationService.authenticate(request))
            .isInstanceOf(InvalidCredentialsException.class)
            .hasMessageContaining("Invalid password");

        verify(userService).findByEmail("user@example.com");
        verify(passwordEncoder).matches("wrongPassword", "encodedPassword");
        verify(jwtService, never()).generateAccessToken(anyString(), anyString());
        verify(jwtService, never()).generateRefreshToken(anyString(), anyString());
    }

    private User createTestUser(String email, String password, Status status, boolean verified) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(password);
        user.setRole(Role.USER);
        user.setStatus(status);
        user.setVerified(verified);
        return user;
    }
}