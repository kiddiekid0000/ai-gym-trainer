package com.aigymtrainer.backend.workoutsession.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.aigymtrainer.backend.workoutset.dto.response.WorkoutSetDto;

public record WorkoutSessionDto(
    Long id,
    LocalDateTime startTime,
    LocalDateTime endTime,
    Integer totalReps,
    Long duration,
    Double averageAccuracy,
    String status,
    List<WorkoutSetDto> sets
) {}