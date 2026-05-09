package com.aigymtrainer.backend.exercise.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "exercises")
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExerciseType type;

    @Column(name = "calories_per_rep", nullable = false)
    private Double caloriesPerRep;

    @Column(nullable = false)
    private String difficulty;

    public Exercise() {}

    public Exercise(String name, ExerciseType type, Double caloriesPerRep, String difficulty) {
        this.name = name;
        this.type = type;
        this.caloriesPerRep = caloriesPerRep;
        this.difficulty = difficulty;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ExerciseType getType() {
        return type;
    }

    public void setType(ExerciseType type) {
        this.type = type;
    }

    public Double getCaloriesPerRep() {
        return caloriesPerRep;
    }

    public void setCaloriesPerRep(Double caloriesPerRep) {
        this.caloriesPerRep = caloriesPerRep;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }
}