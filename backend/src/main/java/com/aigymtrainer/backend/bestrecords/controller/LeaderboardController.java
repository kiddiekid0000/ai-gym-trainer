package com.aigymtrainer.backend.bestrecords.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aigymtrainer.backend.bestrecords.domain.BestRecords;
import com.aigymtrainer.backend.bestrecords.dto.response.LeaderboardEntryDto;
import com.aigymtrainer.backend.bestrecords.service.BestRecordsService;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final BestRecordsService bestRecordsService;

    public LeaderboardController(BestRecordsService bestRecordsService) {
        this.bestRecordsService = bestRecordsService;
    }

    @GetMapping("/{exerciseId}")
    public ResponseEntity<List<LeaderboardEntryDto>> getLeaderboard(@PathVariable Long exerciseId) {
        List<BestRecords> records = bestRecordsService.getLeaderboard(exerciseId);
        
        List<LeaderboardEntryDto> dtos = records.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }

    private LeaderboardEntryDto toDto(BestRecords record) {
        // TODO: Get username from user service
        String username = "User " + record.getUserId(); // Placeholder
        
        return new LeaderboardEntryDto(
            record.getRank(),
            record.getUserId(),
            username,
            record.getTotalReps(),
            record.getAccuracy(),
            record.getStreakDays()
        );
    }
}