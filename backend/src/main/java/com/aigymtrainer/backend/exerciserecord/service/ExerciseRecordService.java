package com.aigymtrainer.backend.exerciserecord.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aigymtrainer.backend.exerciserecord.domain.ExerciseRecord;
import com.aigymtrainer.backend.exerciserecord.repository.ExerciseRecordRepository;

@Service
public class ExerciseRecordService {

    private final ExerciseRecordRepository exerciseRecordRepository;

    public ExerciseRecordService(ExerciseRecordRepository exerciseRecordRepository) {
        this.exerciseRecordRepository = exerciseRecordRepository;
    }

    @Transactional
    public void updateDailyRecord(Long userId, Long exerciseId, LocalDate date, int reps, double accuracy, long duration) {
        Optional<ExerciseRecord> recordOpt = exerciseRecordRepository.findByUserIdAndExerciseIdAndDate(userId, exerciseId, date);
        
        ExerciseRecord record;
        if (recordOpt.isPresent()) {
            record = recordOpt.get();
            record.setTotalReps(record.getTotalReps() + reps);
            record.setSessionsCount(record.getSessionsCount() + 1);
            record.setTotalDuration(record.getTotalDuration() + duration);
            // Update average accuracy
            double totalAccuracy = record.getAverageAccuracy() * record.getSessionsCount() + accuracy;
            record.setAverageAccuracy(totalAccuracy / (record.getSessionsCount() + 1));
            // Update best set if this rep count is higher
            if (reps > record.getBestSet()) {
                record.setBestSet(reps);
            }
        } else {
            record = new ExerciseRecord(userId, exerciseId, date);
            record.setTotalReps(reps);
            record.setBestSet(reps);
            record.setAverageAccuracy(accuracy);
            record.setSessionsCount(1);
            record.setTotalDuration(duration);
        }
        
        exerciseRecordRepository.save(record);
    }

    public List<ExerciseRecord> getUserRecords(Long userId, LocalDate startDate, LocalDate endDate) {
        return exerciseRecordRepository.findByUserIdAndDateRange(userId, startDate, endDate);
    }

    public List<ExerciseRecord> getUserWeeklyRecords(Long userId, LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);
        return getUserRecords(userId, weekStart, weekEnd);
    }

    public List<ExerciseRecord> getUserMonthlyRecords(Long userId, LocalDate monthStart) {
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
        return getUserRecords(userId, monthStart, monthEnd);
    }
}