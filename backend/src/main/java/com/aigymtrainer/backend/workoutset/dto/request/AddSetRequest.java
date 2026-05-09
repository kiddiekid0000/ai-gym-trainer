package com.aigymtrainer.backend.workoutset.dto.request;

public record AddSetRequest(
    Integer repCount,
    Double accuracy,
    Long restDuration
) {}