package com.aigymtrainer.backend.infrastructure.task;

import com.aigymtrainer.backend.common.constant.AuthConstants;
import com.aigymtrainer.backend.user.domain.Status;
import com.aigymtrainer.backend.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class PendingAccountCleanupTask {

    private static final Logger logger = LoggerFactory.getLogger(PendingAccountCleanupTask.class);

    private final UserRepository userRepository;

    public PendingAccountCleanupTask(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Scheduled task to clean up PENDING accounts older than 24 hours.
     * Runs every hour.
     */
    @Scheduled(fixedRate = 3600000) // Run every hour (3600000 ms)
    @Transactional
    public void cleanupPendingAccounts() {
        logger.info("Starting pending account cleanup task");

        try {
            // Calculate the cutoff time (24 hours ago)
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(AuthConstants.PENDING_ACCOUNT_CLEANUP_HOURS);

            // Find all PENDING accounts created before the cutoff time
            List<com.aigymtrainer.backend.user.domain.User> pendingAccounts = 
                    userRepository.findByStatusAndCreatedAtBefore(Status.PENDING, cutoffTime);

            if (pendingAccounts.isEmpty()) {
                logger.info("No pending accounts to clean up");
                return;
            }

            logger.info("Found {} pending accounts older than {} hours", 
                    pendingAccounts.size(), AuthConstants.PENDING_ACCOUNT_CLEANUP_HOURS);

            // Delete all pending accounts
            userRepository.deleteAll(pendingAccounts);

            logger.info("Successfully deleted {} pending accounts", pendingAccounts.size());

        } catch (Exception e) {
            logger.error("Error during pending account cleanup", e);
        }
    }
}
