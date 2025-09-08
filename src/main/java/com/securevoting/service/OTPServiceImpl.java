package com.securevoting.service;

import com.securevoting.exception.OTPGenerationException;
import com.securevoting.model.OTPData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Implementation of OTPService using TOTP (Time-based One-Time Password) algorithm.
 * Provides secure OTP generation with configurable time windows and rate limiting.
 */
@Service
public class OTPServiceImpl implements OTPService {
    
    private static final Logger logger = LoggerFactory.getLogger(OTPServiceImpl.class);
    
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int OTP_LENGTH = 6;
    private static final int[] POWER_OF_10 = {1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000};
    
    // In-memory storage for OTPs (in production, use Redis or database)
    private final ConcurrentMap<String, OTPData> otpStorage = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LocalDateTime> rateLimitTracker = new ConcurrentHashMap<>();
    
    @Value("${otp.validity.minutes:5}")
    private int otpValidityMinutes;
    
    @Value("${otp.rate.limit.minutes:1}")
    private int rateLimitMinutes;
    
    @Value("${otp.max.attempts:3}")
    private int maxAttempts;
    
    @Value("${otp.time.step.seconds:30}")
    private int timeStepSeconds;
    
    private final SecureRandom secureRandom = new SecureRandom();
    
    @Override
    public String generateOTP(String userId) {
        logger.debug("Generating OTP for user: {}", userId);
        
        if (userId == null) {
            logger.warn("Cannot generate OTP: userId is null");
            throw new OTPGenerationException("User ID cannot be null");
        }
        
        if (isRateLimitExceeded(userId)) {
            logger.warn("Rate limit exceeded for user: {}", userId);
            throw new OTPGenerationException("Rate limit exceeded. Please wait before requesting a new OTP.");
        }
        
        try {
            // Generate secret key for this user (in production, use persistent user secret)
            String userSecret = generateUserSecret(userId);
            
            // Calculate current time step
            long timeStep = System.currentTimeMillis() / 1000L / timeStepSeconds;
            
            // Generate TOTP
            String otp = generateTOTP(userSecret, timeStep);
            
            // Store OTP with expiry
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiresAt = now.plusMinutes(otpValidityMinutes);
            
            OTPData otpData = new OTPData(userId, otp, now, expiresAt);
            otpStorage.put(userId, otpData);
            
            // Update rate limit tracker
            rateLimitTracker.put(userId, now);
            
            logger.info("OTP generated successfully for user: {}", userId);
            return otp;
            
        } catch (Exception e) {
            logger.error("Failed to generate OTP for user: {}", userId, e);
            throw new OTPGenerationException("Failed to generate OTP", e);
        }
    }
    
    @Override
    public boolean validateOTP(String userId, String otp) {
        logger.debug("Validating OTP for user: {}", userId);
        
        if (userId == null || otp == null) {
            logger.warn("Invalid input: userId or OTP is null");
            return false;
        }
        
        OTPData otpData = otpStorage.get(userId);
        if (otpData == null) {
            logger.warn("No OTP found for user: {}", userId);
            return false;
        }
        
        // Check if OTP is expired
        if (otpData.isExpired()) {
            logger.warn("OTP expired for user: {}", userId);
            otpStorage.remove(userId);
            return false;
        }
        
        // Check attempt count
        if (otpData.getAttemptCount() >= maxAttempts) {
            logger.warn("Maximum OTP attempts exceeded for user: {}", userId);
            otpStorage.remove(userId);
            return false;
        }
        
        // Increment attempt count
        otpData.incrementAttemptCount();
        
        // Validate OTP with time window tolerance
        boolean isValid = validateOTPWithTimeWindow(userId, otp, otpData);
        
        if (isValid) {
            logger.info("OTP validation successful for user: {}", userId);
            otpStorage.remove(userId); // Remove OTP after successful validation
        } else {
            logger.warn("OTP validation failed for user: {} (attempt {})", userId, otpData.getAttemptCount());
        }
        
        return isValid;
    }
    
    @Override
    public void invalidateOTP(String userId) {
        logger.debug("Invalidating OTP for user: {}", userId);
        otpStorage.remove(userId);
        logger.info("OTP invalidated for user: {}", userId);
    }
    
    @Override
    public void cleanupExpiredOTPs() {
        logger.debug("Starting cleanup of expired OTPs");
        
        LocalDateTime now = LocalDateTime.now();
        int removedCount = 0;
        
        // Clean up expired OTPs
        otpStorage.entrySet().removeIf(entry -> {
            if (entry.getValue().isExpired()) {
                logger.debug("Removing expired OTP for user: {}", entry.getKey());
                return true;
            }
            return false;
        });
        
        // Clean up old rate limit entries
        rateLimitTracker.entrySet().removeIf(entry -> {
            if (entry.getValue().isBefore(now.minusMinutes(rateLimitMinutes))) {
                return true;
            }
            return false;
        });
        
        logger.info("Cleanup completed. Removed {} expired OTPs", removedCount);
    }
    
    @Override
    public boolean isRateLimitExceeded(String userId) {
        if (userId == null) {
            return false;
        }
        
        LocalDateTime lastRequest = rateLimitTracker.get(userId);
        if (lastRequest == null) {
            return false;
        }
        
        LocalDateTime rateLimitExpiry = lastRequest.plusMinutes(rateLimitMinutes);
        return LocalDateTime.now().isBefore(rateLimitExpiry);
    }
    
    /**
     * Generates a TOTP using HMAC-SHA256 algorithm.
     */
    private String generateTOTP(String secret, long timeStep) throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] secretBytes = Base64.getDecoder().decode(secret);
        byte[] timeBytes = ByteBuffer.allocate(8).putLong(timeStep).array();
        
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(secretBytes, HMAC_ALGORITHM);
        mac.init(keySpec);
        
        byte[] hash = mac.doFinal(timeBytes);
        
        // Dynamic truncation
        int offset = hash[hash.length - 1] & 0x0F;
        int truncatedHash = ((hash[offset] & 0x7F) << 24) |
                           ((hash[offset + 1] & 0xFF) << 16) |
                           ((hash[offset + 2] & 0xFF) << 8) |
                           (hash[offset + 3] & 0xFF);
        
        int otp = truncatedHash % POWER_OF_10[OTP_LENGTH];
        
        // Pad with leading zeros if necessary
        return String.format("%0" + OTP_LENGTH + "d", otp);
    }
    
    /**
     * Validates OTP with time window tolerance (current, previous, and next time steps).
     */
    private boolean validateOTPWithTimeWindow(String userId, String inputOtp, OTPData otpData) {
        try {
            String userSecret = generateUserSecret(userId);
            long currentTimeStep = System.currentTimeMillis() / 1000L / timeStepSeconds;
            
            // Check current time step and adjacent time steps for clock skew tolerance
            for (int i = -1; i <= 1; i++) {
                String expectedOtp = generateTOTP(userSecret, currentTimeStep + i);
                if (expectedOtp.equals(inputOtp)) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            logger.error("Error validating OTP for user: {}", userId, e);
            return false;
        }
    }
    
    /**
     * Generates a user-specific secret key.
     * In production, this should be stored securely and retrieved from user data.
     */
    private String generateUserSecret(String userId) {
        // For demo purposes, generate deterministic secret based on userId
        // In production, use a proper secret stored securely for each user
        byte[] userIdBytes = userId.getBytes();
        byte[] secret = new byte[32]; // 256-bit secret
        
        // Simple deterministic generation (replace with proper secret management)
        for (int i = 0; i < secret.length; i++) {
            secret[i] = (byte) (userIdBytes[i % userIdBytes.length] ^ (i * 7));
        }
        
        return Base64.getEncoder().encodeToString(secret);
    }
}