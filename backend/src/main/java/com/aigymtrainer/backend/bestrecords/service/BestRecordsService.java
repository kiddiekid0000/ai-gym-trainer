package com.aigymtrainer.backend.bestrecords.service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aigymtrainer.backend.bestrecords.domain.BestRecords;
import com.aigymtrainer.backend.bestrecords.repository.BestRecordsRepository;

@Service
public class BestRecordsService {

    private final BestRecordsRepository bestRecordsRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    public BestRecordsService(BestRecordsRepository bestRecordsRepository, 
                            RedisTemplate<String, Object> redisTemplate,
                            SimpMessagingTemplate messagingTemplate) {
        this.bestRecordsRepository = bestRecordsRepository;
        this.redisTemplate = redisTemplate;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public void updateBestRecord(Long userId, Long exerciseId, long totalReps, double accuracy) {
        Optional<BestRecords> recordOpt = bestRecordsRepository.findByUserIdAndExerciseId(userId, exerciseId);
        
        BestRecords record;
        int oldRank = 0;
        if (recordOpt.isPresent()) {
            record = recordOpt.get();
            oldRank = record.getRank();
            record.setTotalReps(totalReps);
            record.setAccuracy(accuracy);
            // Update streak logic would go here
        } else {
            record = new BestRecords(exerciseId, userId);
            record.setTotalReps(totalReps);
            record.setAccuracy(accuracy);
        }
        
        bestRecordsRepository.save(record);
        
        // Refresh leaderboard and check for rank changes
        List<BestRecords> leaderboard = bestRecordsRepository.findByExerciseIdOrderByTotalRepsDesc(exerciseId);
        for (int i = 0; i < leaderboard.size(); i++) {
            BestRecords r = leaderboard.get(i);
            int newRank = i + 1;
            if (r.getId().equals(record.getId()) && oldRank != newRank) {
                // Rank changed, send WebSocket update
                messagingTemplate.convertAndSend("/topic/leaderboard/" + exerciseId, 
                    (Object) Map.of("type", "rank_change", "userId", userId, "oldRank", oldRank, "newRank", newRank));
            }
            r.setRank(newRank);
            bestRecordsRepository.save(r);
        }
        
        // Invalidate cache
        invalidateLeaderboardCache(exerciseId);
    }

    public List<BestRecords> getLeaderboard(Long exerciseId) {
        String cacheKey = "leaderboard:exercise:" + exerciseId;
        List<BestRecords> cached = (List<BestRecords>) redisTemplate.opsForValue().get(cacheKey);
        
        if (cached != null) {
            return cached;
        }
        
        List<BestRecords> leaderboard = bestRecordsRepository.findByExerciseIdOrderByTotalRepsDesc(exerciseId);
        // Update ranks
        for (int i = 0; i < leaderboard.size(); i++) {
            leaderboard.get(i).setRank(i + 1);
        }
        
        redisTemplate.opsForValue().set(cacheKey, leaderboard, Duration.ofMinutes(5));
        return leaderboard;
    }

    public List<BestRecords> getTop10Leaderboard(Long exerciseId) {
        return bestRecordsRepository.findTop10ByExerciseId(exerciseId);
    }

    private void invalidateLeaderboardCache(Long exerciseId) {
        String cacheKey = "leaderboard:exercise:" + exerciseId;
        redisTemplate.delete(cacheKey);
    }
}