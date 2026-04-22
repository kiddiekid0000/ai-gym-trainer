package com.aigymtrainer.backend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserRegistrationDto(
    @Email(message = "Email should be valid")
    @NotBlank(message = "Email cannot be blank")
    String email,
    
    @NotBlank(message = "Password cannot be blank")
    @Size(min = 10, message = "Password must be at least 10 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{10,}$", 
             message = "Password must contain at least one lowercase letter, one uppercase letter, one digit, and one special character")
    String password
) {}
