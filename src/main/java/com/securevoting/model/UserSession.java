package com.securevoting.model;

import java.time.LocalDateTime;

/**
 * Model representing a user session in the system.
 * Used for session management and timeout handling.
 */
public class UserSession {
    
    private String sessionId;
    private String userId;
    private LocalDateTime createdAt;
    private LocalDateTime expiryTime;
    private String ipAddress;
    private boolean active;
    
    // Constructors
    public UserSession() {
        this.createdAt = LocalDateTime.now();
        this.active = true;
    }
    
    public UserSession(String sessionId, String userId, LocalDateTime expiryTime) {
        this();
        this.sessionId = sessionId;
        this.userId = userId;
        this.expiryTime = expiryTime;
    }
    
    public UserSession(String sessionId, String userId, LocalDateTime expiryTime, String ipAddress) {
        this(sessionId, userId, expiryTime);
        this.ipAddress = ipAddress;
    }
    
    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getExpiryTime() {
        return expiryTime;
    }
    
    public void setExpiryTime(LocalDateTime expiryTime) {
        this.expiryTime = expiryTime;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    /**
     * Checks if the session has expired.
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryTime);
    }
    
    /**
     * Checks if the session is valid (active and not expired).
     */
    public boolean isValid() {
        return active && !isExpired();
    }
    
    @Override
    public String toString() {
        return "UserSession{" +
                "sessionId='" + sessionId + '\'' +
                ", userId='" + userId + '\'' +
                ", createdAt=" + createdAt +
                ", expiryTime=" + expiryTime +
                ", ipAddress='" + ipAddress + '\'' +
                ", active=" + active +
                '}';
    }
}