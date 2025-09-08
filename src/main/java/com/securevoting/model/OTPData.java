package com.securevoting.model;

import java.time.LocalDateTime;

/**
 * Data model for storing OTP information including generation time and expiry.
 */
public class OTPData {
    
    private String userId;
    private String otpCode;
    private LocalDateTime generatedAt;
    private LocalDateTime expiresAt;
    private int attemptCount;
    
    public OTPData() {}
    
    public OTPData(String userId, String otpCode, LocalDateTime generatedAt, LocalDateTime expiresAt) {
        this.userId = userId;
        this.otpCode = otpCode;
        this.generatedAt = generatedAt;
        this.expiresAt = expiresAt;
        this.attemptCount = 0;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getOtpCode() {
        return otpCode;
    }
    
    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }
    
    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }
    
    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
    
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public int getAttemptCount() {
        return attemptCount;
    }
    
    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }
    
    public void incrementAttemptCount() {
        this.attemptCount++;
    }
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}