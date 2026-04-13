package com.aigymtrainer.backend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRegistrationDto(
    @Email(message = "Email should be valid")
    @NotBlank(message = "Email cannot be blank")
    String email,
    
    @NotBlank(message = "Password cannot be blank")
    @Size(min = 8, message = "Password must be at least 8 characters")
    String password
) {}
