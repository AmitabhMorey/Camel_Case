package com.securevoting.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * AuditLog entity for tracking all system activities and security events.
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog {
    
    @Id
    @Column(name = "log_id", length = 36)
    private String logId;
    
    @Column(name = "user_id", length = 36)
    private String userId;
    
    @NotBlank(message = "Action is required")
    @Size(max = 100, message = "Action cannot exceed 100 characters")
    @Column(name = "action", nullable = false, length = 100)
    private String action;
    
    @Size(max = 1000, message = "Details cannot exceed 1000 characters")
    @Column(name = "details", length = 1000)
    private String details;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private AuditEventType eventType;
    
    @Column(name = "session_id", length = 100)
    private String sessionId;
    
    @Column(name = "resource_id", length = 36)
    private String resourceId;
    
    @Column(name = "resource_type", length = 50)
    private String resourceType;
    
    @Column(name = "integrity_hash", length = 64)
    private String integrityHash;
    
    // Constructors
    public AuditLog() {
        this.timestamp = LocalDateTime.now();
    }
    
    public AuditLog(String logId, String userId, String action, AuditEventType eventType) {
        this();
        this.logId = logId;
        this.userId = userId;
        this.action = action;
        this.eventType = eventType;
    }
    
    // Getters and Setters
    public String getLogId() {
        return logId;
    }
    
    public void setLogId(String logId) {
        this.logId = logId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public String getDetails() {
        return details;
    }
    
    public void setDetails(String details) {
        this.details = details;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public AuditEventType getEventType() {
        return eventType;
    }
    
    public void setEventType(AuditEventType eventType) {
        this.eventType = eventType;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getResourceId() {
        return resourceId;
    }
    
    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }
    
    public String getResourceType() {
        return resourceType;
    }
    
    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }
    
    public String getIntegrityHash() {
        return integrityHash;
    }
    
    public void setIntegrityHash(String integrityHash) {
        this.integrityHash = integrityHash;
    }
    
    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
    
    @Override
    public String toString() {
        return "AuditLog{" +
                "logId='" + logId + '\'' +
                ", userId='" + userId + '\'' +
                ", action='" + action + '\'' +
                ", eventType=" + eventType +
                ", timestamp=" + timestamp +
                ", ipAddress='" + ipAddress + '\'' +
                '}';
    }
}