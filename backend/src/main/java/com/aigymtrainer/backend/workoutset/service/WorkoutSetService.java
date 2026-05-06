package com.aigymtrainer.backend.workoutset.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aigymtrainer.backend.workoutset.domain.WorkoutSet;
import com.aigymtrainer.backend.workoutset.repository.WorkoutSetRepository;

@Service
public class WorkoutSetService {

    private final WorkoutSetRepository workoutSetRepository;

    public WorkoutSetService(WorkoutSetRepository workoutSetRepository) {
        this.workoutSetRepository = workoutSetRepository;
    }

    @Transactional
    public WorkoutSet addSet(Long sessionId, Integer setNumber, Integer repCount, Double accuracy, Long restDuration) {
        WorkoutSet workoutSet = new WorkoutSet(sessionId, setNumber, repCount, accuracy, restDuration);
        return workoutSetRepository.save(workoutSet);
    }

    public List<WorkoutSet> getSetsBySessionId(Long sessionId) {
        return workoutSetRepository.findBySessionIdOrderBySetNumber(sessionId);
    }

    public List<WorkoutSet> getSetsBySessionIds(List<Long> sessionIds) {
        return workoutSetRepository.findBySessionIds(sessionIds);
    }
}