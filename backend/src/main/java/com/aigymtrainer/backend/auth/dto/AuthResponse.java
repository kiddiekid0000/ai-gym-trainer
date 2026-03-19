package com.aigymtrainer.backend.auth.dto;

public record AuthResponse(Long id, String email, String token, String role) {
}