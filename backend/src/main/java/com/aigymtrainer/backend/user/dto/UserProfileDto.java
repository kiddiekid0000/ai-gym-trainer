package com.aigymtrainer.backend.user.dto;

public class UserProfileDto {
    private Long id;
    private String email;
    private String role;
    private boolean verified;
    private String status;

    public UserProfileDto() {}

    public UserProfileDto(Long id, String email, String role, boolean verified, String status) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.verified = verified;
        this.status = status;
    }

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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
