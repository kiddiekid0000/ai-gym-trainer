package com.aigymtrainer.backend.workoutset.dto.response;

import java.time.LocalDateTime;

public record WorkoutSetDto(
    Integer setNumber,
    Integer repCount,
    Double accuracy,
    Long restDuration,
    LocalDateTime completedAt
) {}