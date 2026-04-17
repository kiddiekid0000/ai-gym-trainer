package com.aigymtrainer.backend.user.service;

import com.aigymtrainer.backend.exception.UserNotFoundException;
import com.aigymtrainer.backend.user.domain.Status;
import com.aigymtrainer.backend.user.domain.User;
import com.aigymtrainer.backend.user.dto.UserDto;
import com.aigymtrainer.backend.user.mapper.UserMapper;
import com.aigymtrainer.backend.user.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public User findByEmail(String email) {
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
    @CacheEvict(value = "user_auth_cache", key = "#root.target.findById(#id).email")
    public void updateUserStatus(Long id, Status newStatus) {
        User user = findById(id);
        String email = user.getEmail();
        user.setStatus(newStatus);
        userRepository.save(user);
    }

    @Transactional
    @CacheEvict(value = "user_auth_cache", key = "#email")
    public void updateUserStatusByEmail(String email, Status newStatus) {
        User user = findByEmail(email);
        user.setStatus(newStatus);
        userRepository.save(user);
    }
}
