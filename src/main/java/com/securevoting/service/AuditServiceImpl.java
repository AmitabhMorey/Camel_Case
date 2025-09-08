package com.securevoting.service;

import com.securevoting.model.AuditLog;
import com.securevoting.model.AuditEventType;
import com.securevoting.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of AuditService providing comprehensive audit logging with tamper-evident features.
 */
@Service
@Transactional
public class AuditServiceImpl implements AuditService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuditServiceImpl.class);
    
    @Autowired
    private AuditLogRepository auditLogRepository;
    
    @Autowired
    private CryptographyService cryptographyService;
    
    @Override
    public AuditLog logUserAction(String userId, String action, String details, String ipAddress) {
        logger.info("Logging user action: userId={}, action={}", userId, action);
        
        AuditLog auditLog = new AuditLog();
        auditLog.setLogId(UUID.randomUUID().toString());
        auditLog.setUserId(userId);
        auditLog.setAction(action);
        auditLog.setDetails(details);
        auditLog.setIpAddress(ipAddress);
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setEventType(AuditEventType.USER_ACTION);
        
        // Generate tamper-evident hash
        String hash = generateLogHash(auditLog);
        auditLog.setIntegrityHash(hash);
        
        return auditLogRepository.save(auditLog);
    }
    
    @Override
    public AuditLog logSecurityEvent(AuditEventType eventType, String details, String ipAddress) {
        logger.warn("Logging security event: eventType={}, details={}", eventType, details);
        
        AuditLog auditLog = new AuditLog();
        auditLog.setLogId(UUID.randomUUID().toString());
        auditLog.setUserId("SYSTEM");
        auditLog.setAction("SECURITY_EVENT");
        auditLog.setDetails(details);
        auditLog.setIpAddress(ipAddress);
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setEventType(eventType);
        
        // Generate tamper-evident hash
        String hash = generateLogHash(auditLog);
        auditLog.setIntegrityHash(hash);
        
        return auditLogRepository.save(auditLog);
    }
    
    @Override
    public AuditLog logVotingEvent(String userId, String electionId, AuditEventType eventType, String ipAddress) {
        logger.info("Logging voting event: userId={}, electionId={}, eventType={}", userId, electionId, eventType);
        
        AuditLog auditLog = new AuditLog();
        auditLog.setLogId(UUID.randomUUID().toString());
        auditLog.setUserId(userId);
        auditLog.setAction("VOTING_EVENT");
        auditLog.setDetails("Election: " + electionId + ", Event: " + eventType);
        auditLog.setIpAddress(ipAddress);
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setEventType(eventType);
        
        // Generate tamper-evident hash
        String hash = generateLogHash(auditLog);
        auditLog.setIntegrityHash(hash);
        
        return auditLogRepository.save(auditLog);
    }
    
    @Override
    public AuditLog logAdminAction(String adminId, String action, String target, String ipAddress) {
        logger.info("Logging admin action: adminId={}, action={}, target={}", adminId, action, target);
        
        AuditLog auditLog = new AuditLog();
        auditLog.setLogId(UUID.randomUUID().toString());
        auditLog.setUserId(adminId);
        auditLog.setAction(action);
        auditLog.setDetails("Target: " + target);
        auditLog.setIpAddress(ipAddress);
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setEventType(AuditEventType.ADMIN_ACTION);
        
        // Generate tamper-evident hash
        String hash = generateLogHash(auditLog);
        auditLog.setIntegrityHash(hash);
        
        return auditLogRepository.save(auditLog);
    }
    
    @Override
    public AuditLog logAuthenticationAttempt(String userId, boolean success, String method, String ipAddress) {
        AuditEventType eventType = success ? AuditEventType.AUTH_SUCCESS : AuditEventType.AUTH_FAILURE;
        logger.info("Logging authentication attempt: userId={}, success={}, method={}", userId, success, method);
        
        AuditLog auditLog = new AuditLog();
        auditLog.setLogId(UUID.randomUUID().toString());
        auditLog.setUserId(userId);
        auditLog.setAction("AUTHENTICATION_ATTEMPT");
        auditLog.setDetails("Method: " + method + ", Success: " + success);
        auditLog.setIpAddress(ipAddress);
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setEventType(eventType);
        
        // Generate tamper-evident hash
        String hash = generateLogHash(auditLog);
        auditLog.setIntegrityHash(hash);
        
        return auditLogRepository.save(auditLog);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogs(Map<String, Object> filters) {
        logger.info("Retrieving audit logs with filters: {}", filters);
        
        String userId = (String) filters.get("userId");
        AuditEventType eventType = (AuditEventType) filters.get("eventType");
        LocalDateTime startDate = (LocalDateTime) filters.get("startDate");
        LocalDateTime endDate = (LocalDateTime) filters.get("endDate");
        String ipAddress = (String) filters.get("ipAddress");
        
        return auditLogRepository.findByFilters(userId, eventType, startDate, endDate, ipAddress);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogsByUser(String userId, LocalDateTime startDate, LocalDateTime endDate) {
        logger.info("Retrieving audit logs for user: {}", userId);
        
        if (startDate != null && endDate != null) {
            return auditLogRepository.findByUserIdAndTimestampBetween(userId, startDate, endDate);
        } else {
            return auditLogRepository.findByUserId(userId);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogsByEventType(AuditEventType eventType, LocalDateTime startDate, LocalDateTime endDate) {
        logger.info("Retrieving audit logs for event type: {}", eventType);
        
        if (startDate != null && endDate != null) {
            return auditLogRepository.findByEventTypeAndTimestampBetween(eventType, startDate, endDate);
        } else {
            return auditLogRepository.findByEventType(eventType);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean verifyLogIntegrity(String logId) {
        logger.info("Verifying integrity of audit log: {}", logId);
        
        AuditLog auditLog = auditLogRepository.findById(logId).orElse(null);
        if (auditLog == null) {
            logger.warn("Audit log not found for verification: {}", logId);
            return false;
        }
        
        // Temporarily store the existing hash
        String existingHash = auditLog.getIntegrityHash();
        
        // Generate new hash for comparison
        String calculatedHash = generateLogHash(auditLog);
        
        boolean isIntact = existingHash.equals(calculatedHash);
        
        if (!isIntact) {
            logger.error("Audit log integrity violation detected for logId: {}", logId);
            // Log this as a security event
            logSecurityEvent(AuditEventType.SECURITY_VIOLATION, 
                "Audit log tampering detected for logId: " + logId, "SYSTEM");
        }
        
        return isIntact;
    }
    
    @Override
    public String generateLogHash(AuditLog auditLog) {
        try {
            // Create a string representation of the audit log for hashing
            StringBuilder logData = new StringBuilder();
            logData.append(auditLog.getLogId())
                   .append(auditLog.getUserId())
                   .append(auditLog.getAction())
                   .append(auditLog.getDetails())
                   .append(auditLog.getIpAddress())
                   .append(auditLog.getTimestamp())
                   .append(auditLog.getEventType());
            
            // Use the cryptography service to generate hash
            return cryptographyService.generateHash(logData.toString());
            
        } catch (Exception e) {
            logger.error("Error generating audit log hash", e);
            throw new RuntimeException("Failed to generate audit log hash", e);
        }
    }
}