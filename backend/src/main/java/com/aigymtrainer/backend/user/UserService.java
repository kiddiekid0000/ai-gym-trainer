package com.aigymtrainer.backend.user;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.aigymtrainer.backend.user.dto.UserResponseDto;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<UserResponseDto> getAllUsersAsDto() {
        return getAllUsers().stream()
                .map(user -> new UserResponseDto(
                        user.getId(),
                        user.getEmail(),
                        user.getRole(),
                        user.getStatus(),
                        user.isVerified()
                ))
                .collect(Collectors.toList());
    }

    public void suspendUser(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus(Status.SUSPENDED);
        userRepository.save(user);
    }

    public void activateUser(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus(Status.ACTIVE);
        userRepository.save(user);
    }
}