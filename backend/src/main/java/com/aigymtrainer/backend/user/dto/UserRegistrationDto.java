package com.aigymtrainer.backend.user.dto;

public class UserRegistrationDto {
    private String email;
    private String password;

    // Default constructor
    public UserRegistrationDto() {}

    // Constructor
    public UserRegistrationDto(String email, String password) {
        this.email = email;
        this.password = password;
    }

    // Getters and setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}