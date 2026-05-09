package com.aigymtrainer.backend.exercise.dto.response;

public record ExerciseDto(
    Long id,
    String name,
    String type,
    Double caloriesPerRep,
    String difficulty
) {}