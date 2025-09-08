package com.securevoting.service;

import com.securevoting.model.AuditLog;
import com.securevoting.model.AuditEventType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Service interface for comprehensive audit logging functionality.
 * Provides methods for logging all system activities with tamper-evident features.
 */
public interface AuditService {
    
    /**
     * Log a user action with details
     * @param userId The ID of the user performing the action
     * @param action The action being performed
     * @param details Additional details about the action
     * @param ipAddress The IP address of the user
     * @return The created audit log entry
     */
    AuditLog logUserAction(String userId, String action, String details, String ipAddress);
    
    /**
     * Log a security event
     * @param eventType The type of security event
     * @param details Details about the security event
     * @param ipAddress The IP address associated with the event
     * @return The created audit log entry
     */
    AuditLog logSecurityEvent(AuditEventType eventType, String details, String ipAddress);
    
    /**
     * Log a voting event without revealing vote content
     * @param userId The ID of the user voting
     * @param electionId The ID of the election
     * @param eventType The type of voting event
     * @param ipAddress The IP address of the voter
     * @return The created audit log entry
     */
    AuditLog logVotingEvent(String userId, String electionId, AuditEventType eventType, String ipAddress);
    
    /**
     * Log an admin action
     * @param adminId The ID of the admin performing the action
     * @param action The admin action being performed
     * @param target The target of the admin action
     * @param ipAddress The IP address of the admin
     * @return The created audit log entry
     */
    AuditLog logAdminAction(String adminId, String action, String target, String ipAddress);
    
    /**
     * Log an authentication attempt
     * @param userId The ID of the user attempting authentication
     * @param success Whether the authentication was successful
     * @param method The authentication method used (QR, OTP, etc.)
     * @param ipAddress The IP address of the authentication attempt
     * @return The created audit log entry
     */
    AuditLog logAuthenticationAttempt(String userId, boolean success, String method, String ipAddress);
    
    /**
     * Retrieve audit logs with filtering capabilities
     * @param filters Map of filter criteria (userId, eventType, startDate, endDate, etc.)
     * @return List of audit logs matching the filters
     */
    List<AuditLog> getAuditLogs(Map<String, Object> filters);
    
    /**
     * Retrieve audit logs for a specific user
     * @param userId The user ID to filter by
     * @param startDate Optional start date filter
     * @param endDate Optional end date filter
     * @return List of audit logs for the user
     */
    List<AuditLog> getAuditLogsByUser(String userId, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Retrieve audit logs by event type
     * @param eventType The event type to filter by
     * @param startDate Optional start date filter
     * @param endDate Optional end date filter
     * @return List of audit logs for the event type
     */
    List<AuditLog> getAuditLogsByEventType(AuditEventType eventType, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Verify the integrity of audit logs using tamper-evident features
     * @param logId The ID of the audit log to verify
     * @return True if the log is intact, false if tampered
     */
    boolean verifyLogIntegrity(String logId);
    
    /**
     * Generate a tamper-evident hash for an audit log entry
     * @param auditLog The audit log entry
     * @return The generated hash
     */
    String generateLogHash(AuditLog auditLog);
}