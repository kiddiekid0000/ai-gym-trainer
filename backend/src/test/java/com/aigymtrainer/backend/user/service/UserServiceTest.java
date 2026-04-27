package com.aigymtrainer.backend.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.aigymtrainer.backend.exception.DuplicateEmailException;
import com.aigymtrainer.backend.exception.UserNotFoundException;
import com.aigymtrainer.backend.user.domain.Role;
import com.aigymtrainer.backend.user.domain.Status;
import com.aigymtrainer.backend.user.domain.User;
import com.aigymtrainer.backend.user.dto.UserDto;
import com.aigymtrainer.backend.user.dto.UserRegistrationDto;
import com.aigymtrainer.backend.user.event.UserStatusChangedEvent;
import com.aigymtrainer.backend.user.mapper.UserMapper;
import com.aigymtrainer.backend.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache userAuthCache;

    @InjectMocks
    private UserService userService;

    @Test
    void registerNewUser_shouldCreateUser_whenEmailNotExists() {
        // Given
        UserRegistrationDto dto = new UserRegistrationDto("test@example.com", "Password123!");
        User savedUser = createTestUser(1L, "test@example.com", Status.PENDING);

        given(userRepository.existsByEmail("test@example.com")).willReturn(false);
        given(passwordEncoder.encode("Password123!")).willReturn("encodedPassword");
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        // When
        User result = userService.registerNewUser(dto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getStatus()).isEqualTo(Status.PENDING);
        assertThat(result.getRole()).isEqualTo(Role.USER);
        assertThat(result.isVerified()).isFalse();

        verify(userRepository).existsByEmail("test@example.com");
        verify(passwordEncoder).encode("Password123!");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerNewUser_shouldThrowException_whenEmailAlreadyExists() {
        // Given
        UserRegistrationDto dto = new UserRegistrationDto("existing@example.com", "Password123!");

        given(userRepository.existsByEmail("existing@example.com")).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.registerNewUser(dto))
            .isInstanceOf(DuplicateEmailException.class)
            .hasMessageContaining("existing@example.com");

        verify(userRepository).existsByEmail("existing@example.com");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void findByEmail_shouldReturnUser_whenUserExists() {
        // Given
        User user = createTestUser(1L, "user@example.com", Status.ACTIVE);
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));

        // When
        User result = userService.findByEmail("user@example.com");

        // Then
        assertThat(result).isEqualTo(user);
        verify(userRepository).findByEmail("user@example.com");
    }

    @Test
    void findByEmail_shouldReturnAdminUser_whenAdminEmail() {
        // Given
        ReflectionTestUtils.setField(userService, "adminEmail", "admin@example.com");

        // When
        User result = userService.findByEmail("admin@example.com");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("admin@example.com");
        assertThat(result.getRole()).isEqualTo(Role.ADMIN);
        assertThat(result.getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(result.isVerified()).isTrue();

        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void findByEmail_shouldThrowException_whenUserNotFound() {
        // Given
        given(userRepository.findByEmail("notfound@example.com")).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.findByEmail("notfound@example.com"))
            .isInstanceOf(UserNotFoundException.class)
            .hasMessageContaining("notfound@example.com");

        verify(userRepository).findByEmail("notfound@example.com");
    }

    @Test
    void findById_shouldReturnUser_whenUserExists() {
        // Given
        User user = createTestUser(1L, "user@example.com", Status.ACTIVE);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // When
        User result = userService.findById(1L);

        // Then
        assertThat(result).isEqualTo(user);
        verify(userRepository).findById(1L);
    }

    @Test
    void findById_shouldThrowException_whenUserNotFound() {
        // Given
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.findById(999L))
            .isInstanceOf(UserNotFoundException.class)
            .hasMessageContaining("999");

        verify(userRepository).findById(999L);
    }

    @Test
    void updateUserStatus_shouldUpdateStatusAndPublishEvent_whenUserExists() {
        // Given
        User user = createTestUser(1L, "user@example.com", Status.PENDING);
        UserDto userDto = new UserDto(1L, "user@example.com", "USER", "ACTIVE", true);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.save(any(User.class))).willReturn(user);
        given(cacheManager.getCache("user_auth_cache")).willReturn(userAuthCache);
        given(userMapper.toUserDto(any(User.class))).willReturn(userDto);

        // When
        UserDto result = userService.updateUserStatus(1L, Status.ACTIVE);

        // Then
        assertThat(result).isEqualTo(userDto);
        assertThat(user.getStatus()).isEqualTo(Status.ACTIVE);

        verify(userRepository).findById(1L);
        verify(userRepository).save(user);
        verify(cacheManager.getCache("user_auth_cache")).evict("user@example.com");
        verify(eventPublisher).publishEvent(any(UserStatusChangedEvent.class));
        verify(userMapper).toUserDto(user);
    }

    @Test
    void getAllUsersAsDto_shouldReturnMappedUsers() {
        // Given
        User user1 = createTestUser(1L, "user1@example.com", Status.ACTIVE);
        User user2 = createTestUser(2L, "user2@example.com", Status.PENDING);
        List<User> users = List.of(user1, user2);

        UserDto dto1 = new UserDto(1L, "user1@example.com", "USER", "ACTIVE", true);
        UserDto dto2 = new UserDto(2L, "user2@example.com", "USER", "PENDING", false);

        given(userRepository.findAll()).willReturn(users);
        given(userMapper.toUserDto(user1)).willReturn(dto1);
        given(userMapper.toUserDto(user2)).willReturn(dto2);

        // When
        List<UserDto> result = userService.getAllUsersAsDto();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(dto1, dto2);

        verify(userRepository).findAll();
        verify(userMapper).toUserDto(user1);
        verify(userMapper).toUserDto(user2);
    }

    @Test
    void save_shouldDelegateToRepository() {
        // Given
        User user = createTestUser(1L, "user@example.com", Status.ACTIVE);
        given(userRepository.save(user)).willReturn(user);

        // When
        User result = userService.save(user);

        // Then
        assertThat(result).isEqualTo(user);
        verify(userRepository).save(user);
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