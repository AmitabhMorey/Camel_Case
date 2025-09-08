package com.securevoting.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduled service for cleaning up security-related data structures
 * to prevent memory leaks and maintain system performance.
 */
@Service
public class SecurityCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityCleanupService.class);

    @Autowired
    private RateLimitingFilter rateLimitingFilter;

    @Autowired
    private SecurityEventDetectionFilter securityEventDetectionFilter;

    /**
     * Cleanup rate limiting data every 5 minutes.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void cleanupRateLimitingData() {
        try {
            rateLimitingFilter.cleanupOldEntries();
            logger.debug("Rate limiting cleanup completed successfully");
        } catch (Exception e) {
            logger.error("Error during rate limiting cleanup", e);
        }
    }

    /**
     * Cleanup security event detection data every 10 minutes.
     */
    @Scheduled(fixedRate = 600000) // 10 minutes
    public void cleanupSecurityEventData() {
        try {
            securityEventDetectionFilter.cleanupOldEntries();
            logger.debug("Security event detection cleanup completed successfully");
        } catch (Exception e) {
            logger.error("Error during security event detection cleanup", e);
        }
    }
}