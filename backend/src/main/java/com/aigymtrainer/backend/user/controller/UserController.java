package com.aigymtrainer.backend.user.controller;

import com.aigymtrainer.backend.user.domain.Status;
import com.aigymtrainer.backend.user.dto.AdminDataDto;
import com.aigymtrainer.backend.user.dto.UserProfileDto;
import com.aigymtrainer.backend.user.dto.UserResponseDto;
import com.aigymtrainer.backend.user.mapper.UserMapper;
import com.aigymtrainer.backend.user.service.UserManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserManagementService userManagementService;
    private final UserMapper userMapper;

    public UserController(UserManagementService userManagementService, UserMapper userMapper) {
        this.userManagementService = userManagementService;
        this.userMapper = userMapper;
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('USER')")
    public UserProfileDto getUserProfile(Authentication authentication) {
        logger.info("Get profile request received");
        
        var user = userManagementService.findByEmail(authentication.getName());
        UserProfileDto profile = userMapper.toProfileDto(user);
        // Frontend expects specific status values, not the internal Status enum
        if ("ACTIVE".equals(profile.getStatus())) {
            profile.setStatus("VERIFIED");
        }
        return profile;
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminDataDto getAdminData(Authentication authentication) {
        logger.info("Get admin data request for: {}", authentication.getName());
        
        var user = userManagementService.findByEmail(authentication.getName());
        return userMapper.toAdminDataDto(user);
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponseDto> getAllUsers() {
        logger.info("Get all users request");
        
        return userManagementService.getAllUsersAsDto();
    }

    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponseDto suspendUser(@PathVariable Long id) {
        logger.info("Suspend user request for ID: {}", id);
        
        userManagementService.updateUserStatus(id, Status.SUSPENDED);
        var user = userManagementService.findById(id);
        return userMapper.toResponseDto(user);
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponseDto activateUser(@PathVariable Long id) {
        logger.info("Activate user request for ID: {}", id);
        
        userManagementService.updateUserStatus(id, Status.ACTIVE);
        var user = userManagementService.findById(id);
        return userMapper.toResponseDto(user);
    }
}
