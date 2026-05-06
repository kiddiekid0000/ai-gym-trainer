package com.aigymtrainer.backend.exercise.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.aigymtrainer.backend.exercise.domain.Exercise;
import com.aigymtrainer.backend.exercise.domain.ExerciseType;

@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
    Optional<Exercise> findByType(ExerciseType type);
    boolean existsByType(ExerciseType type);
    List<Exercise> findAllByType(ExerciseType type);
}