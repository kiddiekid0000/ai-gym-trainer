package com.aigymtrainer.backend.workoutset.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.aigymtrainer.backend.workoutset.domain.WorkoutSet;

@Repository
public interface WorkoutSetRepository extends JpaRepository<WorkoutSet, Long> {
    List<WorkoutSet> findBySessionIdOrderBySetNumber(Long sessionId);
    
    @Query("SELECT ws FROM WorkoutSet ws WHERE ws.sessionId IN :sessionIds ORDER BY ws.sessionId, ws.setNumber")
    List<WorkoutSet> findBySessionIds(@Param("sessionIds") List<Long> sessionIds);
}