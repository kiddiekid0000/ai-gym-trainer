package com.aigymtrainer.backend.bestrecords.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.aigymtrainer.backend.bestrecords.domain.BestRecords;

@Repository
public interface BestRecordsRepository extends JpaRepository<BestRecords, Long> {
    List<BestRecords> findByExerciseIdOrderByTotalRepsDesc(Long exerciseId);
    
    Optional<BestRecords> findByUserIdAndExerciseId(Long userId, Long exerciseId);
    
    @Query("SELECT br FROM BestRecords br WHERE br.exerciseId = :exerciseId ORDER BY br.totalReps DESC LIMIT 10")
    List<BestRecords> findTop10ByExerciseId(@Param("exerciseId") Long exerciseId);
}