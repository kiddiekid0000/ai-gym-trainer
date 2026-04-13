package com.aigymtrainer.backend.user.service.impl;

import com.aigymtrainer.backend.common.exception.UserNotFoundException;
import com.aigymtrainer.backend.user.domain.Status;
import com.aigymtrainer.backend.user.domain.User;
import com.aigymtrainer.backend.user.dto.UserResponseDto;
import com.aigymtrainer.backend.user.mapper.UserMapper;
import com.aigymtrainer.backend.user.repository.UserRepository;
import com.aigymtrainer.backend.user.service.UserManagementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserManagementServiceImpl implements UserManagementService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserManagementServiceImpl(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
    }

    @Override
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public List<UserResponseDto> getAllUsersAsDto() {
        return getAllUsers().stream()
                .map(userMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateUserStatus(Long id, Status newStatus) {
        User user = findById(id);
        user.setStatus(newStatus);
        userRepository.save(user);
    }
}
