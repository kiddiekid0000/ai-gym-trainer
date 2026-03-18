package com.aigymtrainer.backend.auth;

public record AuthResponse(Long id, String email, String token) {
}