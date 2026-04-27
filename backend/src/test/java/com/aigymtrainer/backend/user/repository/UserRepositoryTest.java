package com.aigymtrainer.backend.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.aigymtrainer.backend.user.domain.Role;
import com.aigymtrainer.backend.user.domain.Status;
import com.aigymtrainer.backend.user.domain.User;

@SpringBootTest
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByEmail_shouldReturnUser_whenUserExists() {
        // Given
        User user = createTestUser("test@example.com", "password", Status.ACTIVE);
        userRepository.save(user);

        // When
        Optional<User> found = userRepository.findByEmail("test@example.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
        assertThat(found.get().getStatus()).isEqualTo(Status.ACTIVE);
    }

    @Test
    void findByEmail_shouldReturnEmpty_whenUserNotFound() {
        // When
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void existsByEmail_shouldReturnTrue_whenEmailExists() {
        // Given
        User user = createTestUser("existing@example.com", "password", Status.PENDING);
        userRepository.save(user);

        // When
        boolean exists = userRepository.existsByEmail("existing@example.com");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void existsByEmail_shouldReturnFalse_whenEmailNotExists() {
        // When
        boolean exists = userRepository.existsByEmail("new@example.com");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void findById_shouldReturnUser_whenUserExists() {
        // Given
        User user = createTestUser("user@example.com", "password", Status.ACTIVE);
        User savedUser = userRepository.save(user);

        // When
        Optional<User> found = userRepository.findById(savedUser.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void findById_shouldReturnEmpty_whenUserNotFound() {
        // When
        Optional<User> found = userRepository.findById(999L);

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void findAll_shouldReturnAllUsers() {
        // Given
        User user1 = createTestUser("user1@example.com", "password1", Status.ACTIVE);
        User user2 = createTestUser("user2@example.com", "password2", Status.PENDING);
        userRepository.save(user1);
        userRepository.save(user2);

        // When
        List<User> users = userRepository.findAll();

        // Then
        assertThat(users).hasSize(2);
        assertThat(users)
            .extracting(User::getEmail)
            .containsExactlyInAnyOrder("user1@example.com", "user2@example.com");
    }

    @Test
    void save_shouldPersistUser() {
        // Given
        User user = createTestUser("newuser@example.com", "password", Status.PENDING);

        // When
        User saved = userRepository.save(user);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("newuser@example.com");
        assertThat(saved.getStatus()).isEqualTo(Status.PENDING);
        assertThat(saved.getRole()).isEqualTo(Role.USER);
        assertThat(saved.isVerified()).isFalse();
    }

    @Test
    void save_shouldUpdateExistingUser() {
        // Given
        User user = createTestUser("update@example.com", "password", Status.PENDING);
        User saved = userRepository.save(user);
        saved.setStatus(Status.ACTIVE);
        saved.setVerified(true);

        // When
        User updated = userRepository.save(saved);

        // Then
        assertThat(updated.getId()).isEqualTo(saved.getId());
        assertThat(updated.getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(updated.isVerified()).isTrue();
    }

    private User createTestUser(String email, String password, Status status) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(password);
        user.setRole(Role.USER);
        user.setStatus(status);
        user.setVerified(status == Status.ACTIVE);
        return user;
    }
}