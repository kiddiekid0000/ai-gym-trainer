package com.aigymtrainer.backend.exercise.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.aigymtrainer.backend.exercise.domain.Exercise;
import com.aigymtrainer.backend.exercise.domain.ExerciseType;
import com.aigymtrainer.backend.exercise.repository.ExerciseRepository;

@Service
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;

    public ExerciseService(ExerciseRepository exerciseRepository) {
        this.exerciseRepository = exerciseRepository;
    }

    public List<Exercise> getAllExercises() {
        return exerciseRepository.findAll();
    }

    public Optional<Exercise> getExerciseByType(ExerciseType type) {
        return exerciseRepository.findByType(type);
    }

    public List<Exercise> getExercisesByType(ExerciseType type) {
        return exerciseRepository.findAllByType(type);
    }

    public boolean validateExerciseType(ExerciseType type) {
        return exerciseRepository.existsByType(type);
    }
}