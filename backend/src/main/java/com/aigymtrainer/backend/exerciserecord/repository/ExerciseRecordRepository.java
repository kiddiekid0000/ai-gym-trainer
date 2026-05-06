package com.aigymtrainer.backend.exerciserecord.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.aigymtrainer.backend.exerciserecord.domain.ExerciseRecord;

@Repository
public interface ExerciseRecordRepository extends JpaRepository<ExerciseRecord, Long> {
    Optional<ExerciseRecord> findByUserIdAndExerciseIdAndDate(Long userId, Long exerciseId, LocalDate date);
    
    @Query("SELECT er FROM ExerciseRecord er WHERE er.userId = :userId AND er.date BETWEEN :startDate AND :endDate ORDER BY er.date DESC, er.exerciseId")
    List<ExerciseRecord> findByUserIdAndDateRange(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    List<ExerciseRecord> findByUserIdOrderByDateDesc(Long userId);
}