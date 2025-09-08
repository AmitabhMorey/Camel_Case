package com.securevoting.service;

import java.time.LocalDateTime;

/**
 * Result object for authentication operations.
 * Contains authentication status, session information, and error details.
 */
public class AuthenticationResult {
    
    private boolean successful;
    private String sessionId;
    private String errorMessage;
    private LocalDateTime expiryTime;
    private String userId;
    
    // Constructors
    public AuthenticationResult() {}
    
    public AuthenticationResult(boolean successful, String sessionId, String userId, LocalDateTime expiryTime) {
        this.successful = successful;
        this.sessionId = sessionId;
        this.userId = userId;
        this.expiryTime = expiryTime;
    }
    
    public AuthenticationResult(boolean successful, String errorMessage) {
        this.successful = successful;
        this.errorMessage = errorMessage;
    }
    
    // Static factory methods
    public static AuthenticationResult success(String sessionId, String userId, LocalDateTime expiryTime) {
        return new AuthenticationResult(true, sessionId, userId, expiryTime);
    }
    
    public static AuthenticationResult failure(String errorMessage) {
        return new AuthenticationResult(false, errorMessage);
    }
    
    // Getters and Setters
    public boolean isSuccessful() {
        return successful;
    }
    
    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public LocalDateTime getExpiryTime() {
        return expiryTime;
    }
    
    public void setExpiryTime(LocalDateTime expiryTime) {
        this.expiryTime = expiryTime;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    @Override
    public String toString() {
        return "AuthenticationResult{" +
                "successful=" + successful +
                ", sessionId='" + sessionId + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", expiryTime=" + expiryTime +
                ", userId='" + userId + '\'' +
                '}';
    }
}