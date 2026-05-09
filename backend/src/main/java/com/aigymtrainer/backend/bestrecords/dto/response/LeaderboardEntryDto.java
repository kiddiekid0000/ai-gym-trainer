package com.aigymtrainer.backend.bestrecords.dto.response;

public record LeaderboardEntryDto(
    Integer rank,
    Long userId,
    String username,
    Long totalReps,
    Double accuracy,
    Integer streakDays
) {}