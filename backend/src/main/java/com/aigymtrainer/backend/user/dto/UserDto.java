package com.aigymtrainer.backend.user.dto;

public record UserDto(
    Long id,
    String email,
    String role,
    String status,
    boolean verified
) {}
