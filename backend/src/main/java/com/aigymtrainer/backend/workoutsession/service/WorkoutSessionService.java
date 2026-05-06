package com.aigymtrainer.backend.workoutsession.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aigymtrainer.backend.workoutsession.domain.WorkoutSession;
import com.aigymtrainer.backend.workoutsession.repository.WorkoutSessionRepository;

@Service
public class WorkoutSessionService {

    private final WorkoutSessionRepository workoutSessionRepository;

    public WorkoutSessionService(WorkoutSessionRepository workoutSessionRepository) {
        this.workoutSessionRepository = workoutSessionRepository;
    }

    @Transactional
    public WorkoutSession startSession(Long userId) {
        WorkoutSession session = new WorkoutSession(userId);
        return workoutSessionRepository.save(session);
    }

    @Transactional
    public WorkoutSession endSession(Long sessionId, Long userId) {
        Optional<WorkoutSession> sessionOpt = workoutSessionRepository.findByIdAndUserId(sessionId, userId);
        if (sessionOpt.isEmpty()) {
            throw new RuntimeException("Session not found");
        }
        
        WorkoutSession session = sessionOpt.get();
        if (!"IN_PROGRESS".equals(session.getStatus())) {
            throw new RuntimeException("Session already completed");
        }
        
        session.setEndTime(java.time.LocalDateTime.now());
        session.setStatus("COMPLETED");
        // Duration and stats will be calculated by the controller after getting sets
        
        return workoutSessionRepository.save(session);
    }

    public List<WorkoutSession> getUserSessions(Long userId, LocalDate startDate, LocalDate endDate) {
        return workoutSessionRepository.findByUserIdAndDateRange(userId, startDate, endDate);
    }

    public Optional<WorkoutSession> getSessionByIdAndUser(Long sessionId, Long userId) {
        return workoutSessionRepository.findByIdAndUserId(sessionId, userId);
    }
}