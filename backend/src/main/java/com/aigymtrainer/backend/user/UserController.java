package com.aigymtrainer.backend.user;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aigymtrainer.backend.user.dto.UserResponseDto;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('USER')")
    public String getUserProfile() {
        return "User profile data";
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String getAdminData() {
        return "Admin-only data";
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponseDto> getAllUsers() {
        return userService.getAllUsersAsDto();
    }

    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    public String suspendUser(@PathVariable Long id) {
        userService.suspendUser(id);
        return "User suspended";
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public String activateUser(@PathVariable Long id) {
        userService.activateUser(id);
        return "User activated";
    }
}
