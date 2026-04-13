package com.aigymtrainer.backend.user.dto;

public class AdminDataDto {
    private Long id;
    private String email;
    private String role;
    private String status;
    private boolean verified;

    public AdminDataDto() {}

    public AdminDataDto(Long id, String email, String role, String status, boolean verified) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.status = status;
        this.verified = verified;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }
}
