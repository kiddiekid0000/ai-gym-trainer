package com.aigymtrainer.backend.user.service;

import com.aigymtrainer.backend.user.domain.User;
import com.aigymtrainer.backend.user.domain.Status;
import com.aigymtrainer.backend.user.dto.UserResponseDto;
import java.util.List;

public interface UserManagementService {
    User save(User user);
    User findByEmail(String email);
    User findById(Long id);
    List<User> getAllUsers();
    List<UserResponseDto> getAllUsersAsDto();
    void updateUserStatus(Long id, Status newStatus);
}
