package com.aigymtrainer.backend.workoutset.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "workout_sets")
public class WorkoutSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "set_number", nullable = false)
    private Integer setNumber;

    @Column(name = "rep_count", nullable = false)
    private Integer repCount;

    @Column(nullable = false)
    private Double accuracy;

    @Column(name = "rest_duration")
    private Long restDuration = 0L;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    public WorkoutSet() {}

    public WorkoutSet(Long sessionId, Integer setNumber, Integer repCount, Double accuracy, Long restDuration) {
        this.sessionId = sessionId;
        this.setNumber = setNumber;
        this.repCount = repCount;
        this.accuracy = accuracy;
        this.restDuration = restDuration != null ? restDuration : 0L;
        this.completedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Integer getSetNumber() {
        return setNumber;
    }

    public void setSetNumber(Integer setNumber) {
        this.setNumber = setNumber;
    }

    public Integer getRepCount() {
        return repCount;
    }

    public void setRepCount(Integer repCount) {
        this.repCount = repCount;
    }

    public Double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Double accuracy) {
        this.accuracy = accuracy;
    }

    public Long getRestDuration() {
        return restDuration;
    }

    public void setRestDuration(Long restDuration) {
        this.restDuration = restDuration;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}