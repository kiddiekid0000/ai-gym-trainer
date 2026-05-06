package com.aigymtrainer.backend.workoutsession.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.aigymtrainer.backend.workoutsession.domain.WorkoutSession;

@Repository
public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, Long> {
    List<WorkoutSession> findByUserIdAndStatusOrderByStartTimeDesc(Long userId, String status);
    
    @Query("SELECT ws FROM WorkoutSession ws WHERE ws.userId = :userId AND DATE(ws.startTime) BETWEEN :startDate AND :endDate ORDER BY ws.startTime DESC")
    List<WorkoutSession> findByUserIdAndDateRange(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    Optional<WorkoutSession> findByIdAndUserId(Long id, Long userId);
}