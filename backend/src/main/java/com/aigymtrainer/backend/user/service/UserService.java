package com.aigymtrainer.backend.user.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aigymtrainer.backend.auth.service.TokenService;
import com.aigymtrainer.backend.exception.UserNotFoundException;
import com.aigymtrainer.backend.user.domain.Role;
import com.aigymtrainer.backend.user.domain.Status;
import com.aigymtrainer.backend.user.domain.User;
import com.aigymtrainer.backend.user.dto.UserDto;
import com.aigymtrainer.backend.user.mapper.UserMapper;
import com.aigymtrainer.backend.user.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final TokenService tokenService;
    private final CacheManager cacheManager;

    @Value("${admin.email}")
    private String adminEmail;

    public UserService(UserRepository userRepository, UserMapper userMapper, TokenService tokenService, CacheManager cacheManager) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.tokenService = tokenService;
        this.cacheManager = cacheManager;
    }

    public User save(User user) {
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
        String email = user.getEmail();
        user.setStatus(newStatus);
        userRepository.save(user);
        
        // Evict cache for this user
        if (cacheManager.getCache("user_auth_cache") != null) {
            cacheManager.getCache("user_auth_cache").evict(email);
        }
        
        // Kill switch: Delete refresh token if suspending user
        if (newStatus == Status.SUSPENDED) {
            tokenService.deleteRefreshToken(email);
        }
        
        return userMapper.toUserDto(user);
    }
}
