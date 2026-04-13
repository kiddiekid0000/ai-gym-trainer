package com.aigymtrainer.backend.user.controller;

import com.aigymtrainer.backend.user.domain.Status;
import com.aigymtrainer.backend.user.dto.UserDto;
import com.aigymtrainer.backend.user.mapper.UserMapper;
import com.aigymtrainer.backend.user.service.UserService;
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

    private final UserService userService;
    private final UserMapper userMapper;

    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('USER')")
    public UserDto getUserProfile(Authentication authentication) {
        logger.info("Get profile request received");
        
        var user = userService.findByEmail(authentication.getName());
        return userMapper.toUserDto(user);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto getAdminData(Authentication authentication) {
        logger.info("Get admin data request for: {}", authentication.getName());
        
        var user = userService.findByEmail(authentication.getName());
        return userMapper.toUserDto(user);
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserDto> getAllUsers() {
        logger.info("Get all users request");
        
        return userService.getAllUsersAsDto();
    }

    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto suspendUser(@PathVariable Long id) {
        logger.info("Suspend user request for ID: {}", id);
        
        userService.updateUserStatus(id, Status.SUSPENDED);
        var user = userService.findById(id);
        return userMapper.toUserDto(user);
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto activateUser(@PathVariable Long id) {
        logger.info("Activate user request for ID: {}", id);
        
        userService.updateUserStatus(id, Status.ACTIVE);
        var user = userService.findById(id);
        return userMapper.toUserDto(user);
    }
}
