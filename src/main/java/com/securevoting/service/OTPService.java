package com.securevoting.service;

/**
 * Service interface for OTP (One-Time Password) generation and validation.
 * Provides time-based OTP functionality with configurable time windows and rate limiting.
 */
public interface OTPService {
    
    /**
     * Generates a time-based OTP for the specified user.
     * 
     * @param userId the user ID for whom to generate the OTP
     * @return the generated OTP string
     * @throws com.securevoting.exception.OTPGenerationException if OTP generation fails or rate limit exceeded
     */
    String generateOTP(String userId);
    
    /**
     * Validates an OTP for the specified user within the configured time window.
     * 
     * @param userId the user ID to validate the OTP for
     * @param otp the OTP to validate
     * @return true if the OTP is valid, false otherwise
     */
    boolean validateOTP(String userId, String otp);
    
    /**
     * Invalidates any existing OTP for the specified user.
     * 
     * @param userId the user ID whose OTP should be invalidated
     */
    void invalidateOTP(String userId);
    
    /**
     * Cleans up expired OTPs from storage.
     * This method should be called periodically to maintain storage efficiency.
     */
    void cleanupExpiredOTPs();
    
    /**
     * Checks if the user has exceeded the rate limit for OTP generation.
     * 
     * @param userId the user ID to check
     * @return true if rate limit is exceeded, false otherwise
     */
    boolean isRateLimitExceeded(String userId);
}