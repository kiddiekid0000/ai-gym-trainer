package com.aigymtrainer.backend.workoutsession.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aigymtrainer.backend.bestrecords.service.BestRecordsService;
import com.aigymtrainer.backend.exerciserecord.service.ExerciseRecordService;
import com.aigymtrainer.backend.user.service.UserService;
import com.aigymtrainer.backend.workoutsession.domain.WorkoutSession;
import com.aigymtrainer.backend.workoutsession.dto.response.WorkoutSessionDto;
import com.aigymtrainer.backend.workoutsession.service.WorkoutSessionService;
import com.aigymtrainer.backend.workoutset.dto.request.AddSetRequest;
import com.aigymtrainer.backend.workoutset.dto.response.WorkoutSetDto;
import com.aigymtrainer.backend.workoutset.service.WorkoutSetService;

@RestController
@RequestMapping("/api/workouts")
public class WorkoutController {

    private final WorkoutSessionService workoutSessionService;
    private final WorkoutSetService workoutSetService;
    private final ExerciseRecordService exerciseRecordService;
    private final BestRecordsService bestRecordsService;
    private final UserService userService;

    public WorkoutController(WorkoutSessionService workoutSessionService, 
                           WorkoutSetService workoutSetService,
                           ExerciseRecordService exerciseRecordService,
                           BestRecordsService bestRecordsService,
                           UserService userService) {
        this.workoutSessionService = workoutSessionService;
        this.workoutSetService = workoutSetService;
        this.exerciseRecordService = exerciseRecordService;
        this.bestRecordsService = bestRecordsService;
        this.userService = userService;
    }

    @PostMapping("/start")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<WorkoutSessionDto> startWorkout(Authentication authentication) {
        var user = userService.findByEmail(authentication.getName());
        WorkoutSession session = workoutSessionService.startSession(user.getId());
        return ResponseEntity.ok(toDto(session, List.of()));
    }

    @PostMapping("/{sessionId}/sets")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<WorkoutSetDto> addSet(@PathVariable Long sessionId, 
                                               @RequestBody AddSetRequest request,
                                               Authentication authentication) {
        var user = userService.findByEmail(authentication.getName());
        
        // Verify session belongs to user
        Optional<WorkoutSession> sessionOpt = workoutSessionService.getSessionByIdAndUser(sessionId, user.getId());
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        // Get next set number
        List<com.aigymtrainer.backend.workoutset.domain.WorkoutSet> existingSets = 
            workoutSetService.getSetsBySessionId(sessionId);
        int nextSetNumber = existingSets.size() + 1;
        
        var workoutSet = workoutSetService.addSet(sessionId, nextSetNumber, 
                                                request.repCount(), request.accuracy(), request.restDuration());
        
        return ResponseEntity.ok(toSetDto(workoutSet));
    }

    @PutMapping("/{sessionId}/end")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<WorkoutSessionDto> endWorkout(@PathVariable Long sessionId, Authentication authentication) {
        var user = userService.findByEmail(authentication.getName());
        
        // End session
        WorkoutSession session = workoutSessionService.endSession(sessionId, user.getId());
        
        // Get all sets for the session
        List<com.aigymtrainer.backend.workoutset.domain.WorkoutSet> sets = 
            workoutSetService.getSetsBySessionId(sessionId);
        
        // Calculate session stats
        int totalReps = sets.stream().mapToInt(com.aigymtrainer.backend.workoutset.domain.WorkoutSet::getRepCount).sum();
        double avgAccuracy = sets.stream().mapToDouble(com.aigymtrainer.backend.workoutset.domain.WorkoutSet::getAccuracy).average().orElse(0.0);
        long duration = java.time.Duration.between(session.getStartTime(), session.getEndTime()).getSeconds();
        
        session.setTotalReps(totalReps);
        session.setAverageAccuracy(avgAccuracy);
        session.setDuration(duration);
        
        // Update records (assuming exerciseId is passed or derived - for simplicity, assume push_up for now)
        // In real implementation, exerciseId should be part of the session or request
        long exerciseId = 1L; // TODO: Get from request or session
        exerciseRecordService.updateDailyRecord(user.getId(), exerciseId, LocalDate.now(), totalReps, avgAccuracy, duration);
        
        // Update best records
        // Calculate total reps for user across all records
        long totalUserReps = totalReps; // TODO: Calculate from all records
        bestRecordsService.updateBestRecord(user.getId(), exerciseId, totalUserReps, avgAccuracy);
        
        List<WorkoutSetDto> setDtos = sets.stream().map(this::toSetDto).collect(Collectors.toList());
        return ResponseEntity.ok(toDto(session, setDtos));
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<WorkoutSessionDto>> getUserWorkouts(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            Authentication authentication) {
        var user = userService.findByEmail(authentication.getName());
        
        LocalDate start = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        
        List<WorkoutSession> sessions = workoutSessionService.getUserSessions(user.getId(), start, end);
        List<Long> sessionIds = sessions.stream().map(WorkoutSession::getId).collect(Collectors.toList());
        List<com.aigymtrainer.backend.workoutset.domain.WorkoutSet> allSets = 
            workoutSetService.getSetsBySessionIds(sessionIds);
        
        List<WorkoutSessionDto> dtos = sessions.stream()
            .map(session -> {
                List<WorkoutSetDto> sessionSets = allSets.stream()
                    .filter(set -> set.getSessionId().equals(session.getId()))
                    .map(this::toSetDto)
                    .collect(Collectors.toList());
                return toDto(session, sessionSets);
            })
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }

    private WorkoutSessionDto toDto(WorkoutSession session, List<WorkoutSetDto> sets) {
        return new WorkoutSessionDto(
            session.getId(),
            session.getStartTime(),
            session.getEndTime(),
            session.getTotalReps(),
            session.getDuration(),
            session.getAverageAccuracy(),
            session.getStatus(),
            sets
        );
    }

    private WorkoutSetDto toSetDto(com.aigymtrainer.backend.workoutset.domain.WorkoutSet set) {
        return new WorkoutSetDto(
            set.getSetNumber(),
            set.getRepCount(),
            set.getAccuracy(),
            set.getRestDuration(),
            set.getCompletedAt()
        );
    }
}