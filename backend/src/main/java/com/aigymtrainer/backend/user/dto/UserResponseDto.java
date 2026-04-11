package com.aigymtrainer.backend.user.dto;

import com.aigymtrainer.backend.user.Role;
import com.aigymtrainer.backend.user.Status;

public class UserResponseDto {
    
    private Long id;
    private String email;
    private Role role;
    private Status status;
    private boolean verified;

    public UserResponseDto() {
    }

    public UserResponseDto(Long id, String email, Role role, Status status, boolean verified) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.status = status;
        this.verified = verified;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }
}
