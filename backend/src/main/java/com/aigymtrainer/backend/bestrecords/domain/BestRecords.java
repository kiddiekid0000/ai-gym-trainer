package com.aigymtrainer.backend.bestrecords.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "best_records")
public class BestRecords {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exercise_id", nullable = false)
    private Long exerciseId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "total_reps", nullable = false)
    private Long totalReps = 0L;

    @Column(nullable = false)
    private Double accuracy = 0.0;

    @Column(name = "streak_days", nullable = false)
    private Integer streakDays = 0;

    @Column(nullable = false)
    private Integer rank = 0;

    public BestRecords() {}

    public BestRecords(Long exerciseId, Long userId) {
        this.exerciseId = exerciseId;
        this.userId = userId;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getExerciseId() {
        return exerciseId;
    }

    public void setExerciseId(Long exerciseId) {
        this.exerciseId = exerciseId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getTotalReps() {
        return totalReps;
    }

    public void setTotalReps(Long totalReps) {
        this.totalReps = totalReps;
    }

    public Double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Double accuracy) {
        this.accuracy = accuracy;
    }

    public Integer getStreakDays() {
        return streakDays;
    }

    public void setStreakDays(Integer streakDays) {
        this.streakDays = streakDays;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }
}