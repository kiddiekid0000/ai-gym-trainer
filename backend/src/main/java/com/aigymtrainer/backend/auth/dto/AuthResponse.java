package com.aigymtrainer.backend.auth.dto;

public record AuthResponse(
    Long id,
    String email,
    String role,
    String status,
    String message
) {
    public AuthResponse(Long id, String email, String role, String status) {
        this(id, email, role, status, null);
    }
}
