package com.aigymtrainer.backend.exercise.controller;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aigymtrainer.backend.exercise.domain.Exercise;
import com.aigymtrainer.backend.exercise.domain.ExerciseType;
import com.aigymtrainer.backend.exercise.dto.response.ExerciseDto;
import com.aigymtrainer.backend.exercise.service.ExerciseService;

@RestController
@RequestMapping("/api/exercises")
public class ExerciseController {

    private final ExerciseService exerciseService;

    public ExerciseController(ExerciseService exerciseService) {
        this.exerciseService = exerciseService;
    }

    @GetMapping("/types")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<String>> getExerciseTypes(Authentication authentication) {
        List<String> types = Arrays.stream(ExerciseType.values())
            .map(Enum::name)
            .collect(Collectors.toList());
        return ResponseEntity.ok(types);
    }

    @GetMapping("/type/{type}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<ExerciseDto>> getExercisesByType(@PathVariable ExerciseType type, Authentication authentication) {
        List<Exercise> exercises = exerciseService.getExercisesByType(type);
        List<ExerciseDto> dtos = exercises.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    private ExerciseDto toDto(Exercise exercise) {
        return new ExerciseDto(
            exercise.getId(),
            exercise.getName(),
            exercise.getType().name(),
            exercise.getCaloriesPerRep(),
            exercise.getDifficulty()
        );
    }
}