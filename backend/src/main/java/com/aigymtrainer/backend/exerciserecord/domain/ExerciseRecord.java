package com.aigymtrainer.backend.exerciserecord.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "exercise_records")
public class ExerciseRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "exercise_id", nullable = false)
    private Long exerciseId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "total_reps", nullable = false)
    private Integer totalReps = 0;

    @Column(name = "best_set", nullable = false)
    private Integer bestSet = 0;

    @Column(name = "average_accuracy", nullable = false)
    private Double averageAccuracy = 0.0;

    @Column(name = "sessions_count", nullable = false)
    private Integer sessionsCount = 0;

    @Column(name = "total_duration", nullable = false)
    private Long totalDuration = 0L;

    public ExerciseRecord() {}

    public ExerciseRecord(Long userId, Long exerciseId, LocalDate date) {
        this.userId = userId;
        this.exerciseId = exerciseId;
        this.date = date;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getExerciseId() {
        return exerciseId;
    }

    public void setExerciseId(Long exerciseId) {
        this.exerciseId = exerciseId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Integer getTotalReps() {
        return totalReps;
    }

    public void setTotalReps(Integer totalReps) {
        this.totalReps = totalReps;
    }

    public Integer getBestSet() {
        return bestSet;
    }

    public void setBestSet(Integer bestSet) {
        this.bestSet = bestSet;
    }

    public Double getAverageAccuracy() {
        return averageAccuracy;
    }

    public void setAverageAccuracy(Double averageAccuracy) {
        this.averageAccuracy = averageAccuracy;
    }

    public Integer getSessionsCount() {
        return sessionsCount;
    }

    public void setSessionsCount(Integer sessionsCount) {
        this.sessionsCount = sessionsCount;
    }

    public Long getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(Long totalDuration) {
        this.totalDuration = totalDuration;
    }
}