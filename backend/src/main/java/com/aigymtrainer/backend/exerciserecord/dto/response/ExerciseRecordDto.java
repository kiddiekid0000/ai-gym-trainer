package com.aigymtrainer.backend.exerciserecord.dto.response;

import java.time.LocalDate;

public record ExerciseRecordDto(
    Long exerciseId,
    String exerciseName,
    LocalDate date,
    Integer totalReps,
    Integer bestSet,
    Double averageAccuracy,
    Integer sessionsCount,
    Long totalDuration
) {}