package com.aigymtrainer.backend.user.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final CacheManager cacheManager;

    @Value("${admin.email}")
    private String adminEmail;

    public UserService(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder, ApplicationEventPublisher eventPublisher, CacheManager cacheManager) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.cacheManager = cacheManager;
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public User registerNewUser(UserRegistrationDto userDto) {
        // Check if email already exists
        if (userRepository.existsByEmail(userDto.email())) {
            throw new DuplicateEmailException(userDto.email());
        }

        // Create new user with PENDING status (awaiting OTP verification)
        User user = new User();
        user.setEmail(userDto.email());
        user.setPassword(passwordEncoder.encode(userDto.password()));
        user.setRole(Role.USER);
        user.setStatus(Status.PENDING); // PENDING until OTP is verified
        user.setVerified(false);

        // Save user to database
        return userRepository.save(user);
    }

    public User findByEmail(String email) {
        // Handle admin user specially - admin doesn't exist in database
        if (email.equals(adminEmail)) {
            User adminUser = new User();
            adminUser.setEmail(adminEmail);
            adminUser.setRole(Role.ADMIN);
            adminUser.setStatus(Status.ACTIVE);
            adminUser.setVerified(true);
            return adminUser;
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<UserDto> getAllUsersAsDto() {
        return getAllUsers().stream()
                .map(userMapper::toUserDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserDto updateUserStatus(Long id, Status newStatus) {
        User user = findById(id);
        Status oldStatus = user.getStatus();
        String email = user.getEmail();
        user.setStatus(newStatus);
        userRepository.save(user);
        
        // Evict cache for this user
        if (cacheManager.getCache("user_auth_cache") != null) {
            cacheManager.getCache("user_auth_cache").evict(email);
        }
        
        // Publish event for status change (e.g., to revoke tokens if suspended)
        eventPublisher.publishEvent(new UserStatusChangedEvent(email, oldStatus, newStatus));
        
        return userMapper.toUserDto(user);
    }
}
