package com.aigymtrainer.backend.user.repository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.aigymtrainer.backend.user.domain.Role;
import com.aigymtrainer.backend.user.domain.Status;
import com.aigymtrainer.backend.user.domain.User;

@SpringBootTest(properties = "spring.profiles.active=test") 
@ActiveProfiles("test")
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByEmail_shouldReturnUser_whenUserExists() {
        User user = createTestUser("test@example.com", "password", Status.ACTIVE);
        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("test@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
        assertThat(found.get().getStatus()).isEqualTo(Status.ACTIVE);
    }

    @Test
    void findByEmail_shouldReturnEmpty_whenUserNotFound() {
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");
        assertThat(found).isEmpty();
    }

    @Test
    void existsByEmail_shouldReturnTrue_whenEmailExists() {
        User user = createTestUser("existing@example.com", "password", Status.PENDING);
        userRepository.save(user);

        boolean exists = userRepository.existsByEmail("existing@example.com");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByEmail_shouldReturnFalse_whenEmailNotExists() {
        boolean exists = userRepository.existsByEmail("new@example.com");
        assertThat(exists).isFalse();
    }

    @Test
    void findById_shouldReturnUser_whenUserExists() {
        User user = createTestUser("user@example.com", "password", Status.ACTIVE);
        User savedUser = userRepository.save(user);

        Optional<User> found = userRepository.findById(savedUser.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void findById_shouldReturnEmpty_whenUserNotFound() {
        Optional<User> found = userRepository.findById(999L);
        assertThat(found).isEmpty();
    }

    @Test
    void findAll_shouldReturnAllUsers() {
        User user1 = createTestUser("user1@example.com", "password1", Status.ACTIVE);
        User user2 = createTestUser("user2@example.com", "password2", Status.PENDING);
        userRepository.save(user1);
        userRepository.save(user2);

        List<User> users = userRepository.findAll();

        assertThat(users).hasSize(2);
        assertThat(users)
            .extracting(User::getEmail)
            .containsExactlyInAnyOrder("user1@example.com", "user2@example.com");
    }

    @Test
    void save_shouldPersistUser() {
        User user = createTestUser("newuser@example.com", "password", Status.PENDING);

        User saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("newuser@example.com");
        assertThat(saved.getStatus()).isEqualTo(Status.PENDING);
        assertThat(saved.getRole()).isEqualTo(Role.USER);
        assertThat(saved.isVerified()).isFalse();
    }

    @Test
    void save_shouldUpdateExistingUser() {
        User user = createTestUser("update@example.com", "password", Status.PENDING);
        User saved = userRepository.save(user);
        saved.setStatus(Status.ACTIVE);
        saved.setVerified(true);

        User updated = userRepository.save(saved);

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