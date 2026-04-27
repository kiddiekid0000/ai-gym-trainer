package com.aigymtrainer.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.BDDMockito.given;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aigymtrainer.backend.auth.dto.AuthResult;
import com.aigymtrainer.backend.user.domain.Role;
import com.aigymtrainer.backend.user.domain.Status;
import com.aigymtrainer.backend.user.domain.User;
import com.aigymtrainer.backend.user.dto.UserRegistrationDto;
import com.aigymtrainer.backend.user.service.UserService;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private OtpVerificationService otpVerificationService;

    @InjectMocks
    private RegistrationService registrationService;

    @Test
    void register_shouldCreateUserAndSendOtp_whenSuccessful() {
        // Given
        UserRegistrationDto dto = new UserRegistrationDto("test@example.com", "Password123!");
        User user = createTestUser(1L, "test@example.com", Status.PENDING);

        given(userService.registerNewUser(dto)).willReturn(user);

        // When
        AuthResult result = registrationService.register(dto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.user()).isEqualTo(user);
        assertThat(result.tokens()).isNull(); // No tokens for pending verification

        verify(userService).registerNewUser(dto);
        verify(otpVerificationService).sendOtp("test@example.com");
    }

    @Test
    void register_shouldReturnResultEvenWhenOtpFails() {
        // Given
        UserRegistrationDto dto = new UserRegistrationDto("test@example.com", "Password123!");
        User user = createTestUser(1L, "test@example.com", Status.PENDING);

        given(userService.registerNewUser(dto)).willReturn(user);
        doThrow(new RuntimeException("Email service unavailable"))
            .when(otpVerificationService).sendOtp("test@example.com");

        // When
        AuthResult result = registrationService.register(dto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.user()).isEqualTo(user);
        assertThat(result.tokens()).isNull();

        verify(userService).registerNewUser(dto);
        verify(otpVerificationService).sendOtp("test@example.com");
    }

    @Test
    void register_shouldHandleDifferentUserData() {
        // Given
        UserRegistrationDto dto = new UserRegistrationDto("john.doe@example.com", "SecurePass456!");
        User user = createTestUser(2L, "john.doe@example.com", Status.PENDING);

        given(userService.registerNewUser(dto)).willReturn(user);

        // When
        AuthResult result = registrationService.register(dto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.user().getEmail()).isEqualTo("john.doe@example.com");
        assertThat(result.user().getId()).isEqualTo(2L);

        verify(userService).registerNewUser(dto);
        verify(otpVerificationService).sendOtp("john.doe@example.com");
    }

    private User createTestUser(Long id, String email, Status status) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPassword("encodedPassword");
        user.setRole(Role.USER);
        user.setStatus(status);
        user.setVerified(false);
        return user;
    }
}