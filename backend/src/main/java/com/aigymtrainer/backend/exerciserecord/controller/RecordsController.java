package com.aigymtrainer.backend.exerciserecord.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aigymtrainer.backend.exerciserecord.domain.ExerciseRecord;
import com.aigymtrainer.backend.exerciserecord.dto.response.ExerciseRecordDto;
import com.aigymtrainer.backend.exerciserecord.service.ExerciseRecordService;
import com.aigymtrainer.backend.user.service.UserService;

@RestController
@RequestMapping("/api/records")
public class RecordsController {

    private final ExerciseRecordService exerciseRecordService;
    private final UserService userService;

    public RecordsController(ExerciseRecordService exerciseRecordService, UserService userService) {
        this.exerciseRecordService = exerciseRecordService;
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ExerciseRecordDto>> getUserRecords(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) String period,
            Authentication authentication) {
        
        var user = userService.findByEmail(authentication.getName());
        
        List<ExerciseRecord> records;
        if ("week".equals(period)) {
            LocalDate weekStart = startDate != null ? startDate : LocalDate.now().with(java.time.DayOfWeek.MONDAY);
            records = exerciseRecordService.getUserWeeklyRecords(user.getId(), weekStart);
        } else if ("month".equals(period)) {
            LocalDate monthStart = startDate != null ? startDate : LocalDate.now().withDayOfMonth(1);
            records = exerciseRecordService.getUserMonthlyRecords(user.getId(), monthStart);
        } else {
            LocalDate start = startDate != null ? startDate : LocalDate.now().minusDays(30);
            LocalDate end = endDate != null ? endDate : LocalDate.now();
            records = exerciseRecordService.getUserRecords(user.getId(), start, end);
        }
        
        List<ExerciseRecordDto> dtos = records.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }

    private ExerciseRecordDto toDto(ExerciseRecord record) {
        // TODO: Get exercise name from exercise service
        String exerciseName = "Exercise " + record.getExerciseId(); // Placeholder
        
        return new ExerciseRecordDto(
            record.getExerciseId(),
            exerciseName,
            record.getDate(),
            record.getTotalReps(),
            record.getBestSet(),
            record.getAverageAccuracy(),
            record.getSessionsCount(),
            record.getTotalDuration()
        );
    }
}