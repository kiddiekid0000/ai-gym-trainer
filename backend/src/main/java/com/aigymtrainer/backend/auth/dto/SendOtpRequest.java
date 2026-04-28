package com.aigymtrainer.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendOtpRequest(
    @Email(message = "Email should be valid")
    @NotBlank(message = "Email cannot be blank")
    String email
) {}
