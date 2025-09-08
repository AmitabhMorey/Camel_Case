package com.securevoting.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service for scheduled cleanup of expired OTPs and rate limit entries.
 */
@Service
public class OTPCleanupService {
    
    private static final Logger logger = LoggerFactory.getLogger(OTPCleanupService.class);
    
    @Autowired
    private OTPService otpService;
    
    /**
     * Scheduled cleanup task that runs every 5 minutes to clean up expired OTPs.
     * This helps maintain memory efficiency and removes stale data.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes in milliseconds
    public void cleanupExpiredOTPs() {
        logger.debug("Starting scheduled OTP cleanup");
        try {
            otpService.cleanupExpiredOTPs();
            logger.debug("Scheduled OTP cleanup completed successfully");
        } catch (Exception e) {
            logger.error("Error during scheduled OTP cleanup", e);
        }
    }
}