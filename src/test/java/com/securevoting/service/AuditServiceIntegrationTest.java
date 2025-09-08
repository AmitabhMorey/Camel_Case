package com.securevoting.service;

import com.securevoting.model.AuditEventType;
import com.securevoting.model.AuditLog;
import com.securevoting.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for AuditService.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuditServiceIntegrationTest {
    
    @Autowired
    private AuditService auditService;
    
    @Autowired
    private AuditLogRepository auditLogRepository;
    
    private String testUserId;
    private String testIpAddress;
    
    @BeforeEach
    void setUp() {
        testUserId = "integration-test-user";
        testIpAddress = "192.168.1.200";
        
        // Clean up any existing test data
        auditLogRepository.deleteAll();
    }
    
    @Test
    void testCompleteAuditWorkflow() {
        // Test user action logging
        AuditLog userActionLog = auditService.logUserAction(
                testUserId, "LOGIN", "User logged in", testIpAddress);
        
        assertNotNull(userActionLog);
        assertNotNull(userActionLog.getLogId());
        assertEquals(testUserId, userActionLog.getUserId());
        assertEquals("LOGIN", userActionLog.getAction());
        assertEquals(AuditEventType.USER_ACTION, userActionLog.getEventType());
        assertNotNull(userActionLog.getIntegrityHash());
        
        // Test security event logging
        AuditLog securityLog = auditService.logSecurityEvent(
                AuditEventType.SECURITY_VIOLATION, "Suspicious activity detected", testIpAddress);
        
        assertNotNull(securityLog);
        assertEquals("SYSTEM", securityLog.getUserId());
        assertEquals(AuditEventType.SECURITY_VIOLATION, securityLog.getEventType());
        
        // Test voting event logging
        String electionId = "test-election-123";
        AuditLog votingLog = auditService.logVotingEvent(
                testUserId, electionId, AuditEventType.VOTE_SUCCESS, testIpAddress);
        
        assertNotNull(votingLog);
        assertEquals(testUserId, votingLog.getUserId());
        assertEquals(AuditEventType.VOTE_SUCCESS, votingLog.getEventType());
        assertTrue(votingLog.getDetails().contains(electionId));
        
        // Test admin action logging
        String adminId = "admin-user";
        AuditLog adminLog = auditService.logAdminAction(
                adminId, "CREATE_ELECTION", "election-456", testIpAddress);
        
        assertNotNull(adminLog);
        assertEquals(adminId, adminLog.getUserId());
        assertEquals(AuditEventType.ADMIN_ACTION, adminLog.getEventType());
        
        // Test authentication logging
        AuditLog authSuccessLog = auditService.logAuthenticationAttempt(
                testUserId, true, "QR_CODE", testIpAddress);
        
        assertNotNull(authSuccessLog);
        assertEquals(AuditEventType.AUTH_SUCCESS, authSuccessLog.getEventType());
        
        AuditLog authFailureLog = auditService.logAuthenticationAttempt(
                testUserId, false, "OTP", testIpAddress);
        
        assertNotNull(authFailureLog);
        assertEquals(AuditEventType.AUTH_FAILURE, authFailureLog.getEventType());
        
        // Verify all logs were persisted
        List<AuditLog> allLogs = auditLogRepository.findAll();
        assertEquals(6, allLogs.size());
    }
    
    @Test
    void testAuditLogRetrieval() {
        // Create test data
        auditService.logUserAction(testUserId, "LOGIN", "Login action", testIpAddress);
        auditService.logUserAction(testUserId, "LOGOUT", "Logout action", testIpAddress);
        auditService.logSecurityEvent(AuditEventType.SECURITY_VIOLATION, "Security issue", testIpAddress);
        
        String otherUserId = "other-user";
        auditService.logUserAction(otherUserId, "LOGIN", "Other user login", testIpAddress);
        
        // Test retrieval by user
        List<AuditLog> userLogs = auditService.getAuditLogsByUser(testUserId, null, null);
        assertEquals(2, userLogs.size());
        assertTrue(userLogs.stream().allMatch(log -> testUserId.equals(log.getUserId())));
        
        // Test retrieval by event type
        List<AuditLog> userActionLogs = auditService.getAuditLogsByEventType(
                AuditEventType.USER_ACTION, null, null);
        assertEquals(3, userActionLogs.size());
        
        List<AuditLog> securityLogs = auditService.getAuditLogsByEventType(
                AuditEventType.SECURITY_VIOLATION, null, null);
        assertEquals(1, securityLogs.size());
        
        // Test retrieval with date range
        LocalDateTime startDate = LocalDateTime.now().minusHours(1);
        LocalDateTime endDate = LocalDateTime.now().plusHours(1);
        
        List<AuditLog> dateRangeLogs = auditService.getAuditLogsByUser(testUserId, startDate, endDate);
        assertEquals(2, dateRangeLogs.size());
        
        // Test retrieval with filters
        Map<String, Object> filters = new HashMap<>();
        filters.put("userId", testUserId);
        filters.put("eventType", AuditEventType.USER_ACTION);
        
        List<AuditLog> filteredLogs = auditService.getAuditLogs(filters);
        assertEquals(2, filteredLogs.size());
        assertTrue(filteredLogs.stream().allMatch(log -> 
                testUserId.equals(log.getUserId()) && 
                AuditEventType.USER_ACTION.equals(log.getEventType())));
    }
    
    @Test
    void testLogIntegrityVerification() {
        // Create a test log
        AuditLog originalLog = auditService.logUserAction(
                testUserId, "TEST_ACTION", "Test details", testIpAddress);
        
        String logId = originalLog.getLogId();
        
        // Verify integrity of untampered log
        assertTrue(auditService.verifyLogIntegrity(logId));
        
        // Simulate tampering by modifying the log directly in database
        AuditLog tamperedLog = auditLogRepository.findById(logId).orElseThrow();
        tamperedLog.setDetails("TAMPERED DETAILS");
        auditLogRepository.save(tamperedLog);
        
        // Verify integrity fails for tampered log
        assertFalse(auditService.verifyLogIntegrity(logId));
        
        // Verify that a security event was logged for the tampering detection
        List<AuditLog> securityEvents = auditService.getAuditLogsByEventType(
                AuditEventType.SECURITY_VIOLATION, null, null);
        
        assertTrue(securityEvents.size() > 0);
        assertTrue(securityEvents.stream().anyMatch(log -> 
                log.getDetails().contains("tampering detected")));
    }
    
    @Test
    void testHashGeneration() {
        // Create test log
        AuditLog testLog = new AuditLog();
        testLog.setLogId("test-log-123");
        testLog.setUserId(testUserId);
        testLog.setAction("TEST_ACTION");
        testLog.setDetails("Test details");
        testLog.setIpAddress(testIpAddress);
        testLog.setTimestamp(LocalDateTime.now());
        testLog.setEventType(AuditEventType.USER_ACTION);
        
        // Generate hash
        String hash1 = auditService.generateLogHash(testLog);
        assertNotNull(hash1);
        assertFalse(hash1.isEmpty());
        
        // Generate hash again with same data - should be identical
        String hash2 = auditService.generateLogHash(testLog);
        assertEquals(hash1, hash2);
        
        // Modify log and generate hash - should be different
        testLog.setDetails("Modified details");
        String hash3 = auditService.generateLogHash(testLog);
        assertNotEquals(hash1, hash3);
    }
    
    @Test
    void testConcurrentAuditLogging() throws InterruptedException {
        int numberOfThreads = 10;
        int logsPerThread = 5;
        Thread[] threads = new Thread[numberOfThreads];
        
        // Create multiple threads that log concurrently
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < logsPerThread; j++) {
                    auditService.logUserAction(
                            "user-" + threadId, 
                            "ACTION_" + j, 
                            "Thread " + threadId + " action " + j, 
                            testIpAddress);
                }
            });
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Verify all logs were created
        List<AuditLog> allLogs = auditLogRepository.findAll();
        assertEquals(numberOfThreads * logsPerThread, allLogs.size());
        
        // Verify all logs have unique IDs and valid hashes
        long uniqueIds = allLogs.stream().map(AuditLog::getLogId).distinct().count();
        assertEquals(numberOfThreads * logsPerThread, uniqueIds);
        
        assertTrue(allLogs.stream().allMatch(log -> 
                log.getIntegrityHash() != null && !log.getIntegrityHash().isEmpty()));
    }
    
    @Test
    void testAuditLogFiltering() {
        // Create diverse test data
        LocalDateTime baseTime = LocalDateTime.now();
        
        auditService.logUserAction("user1", "LOGIN", "User1 login", "192.168.1.1");
        auditService.logUserAction("user2", "LOGIN", "User2 login", "192.168.1.2");
        auditService.logSecurityEvent(AuditEventType.SECURITY_VIOLATION, "Security issue", "192.168.1.3");
        auditService.logVotingEvent("user1", "election1", AuditEventType.VOTE_SUCCESS, "192.168.1.1");
        
        // Test filtering by user only
        Map<String, Object> userFilter = new HashMap<>();
        userFilter.put("userId", "user1");
        
        List<AuditLog> user1Logs = auditService.getAuditLogs(userFilter);
        assertEquals(2, user1Logs.size());
        assertTrue(user1Logs.stream().allMatch(log -> "user1".equals(log.getUserId())));
        
        // Test filtering by event type only
        Map<String, Object> eventFilter = new HashMap<>();
        eventFilter.put("eventType", AuditEventType.USER_ACTION);
        
        List<AuditLog> userActionLogs = auditService.getAuditLogs(eventFilter);
        assertEquals(2, userActionLogs.size());
        
        // Test filtering by IP address
        Map<String, Object> ipFilter = new HashMap<>();
        ipFilter.put("ipAddress", "192.168.1.1");
        
        List<AuditLog> ipLogs = auditService.getAuditLogs(ipFilter);
        assertEquals(2, ipLogs.size());
        
        // Test combined filtering
        Map<String, Object> combinedFilter = new HashMap<>();
        combinedFilter.put("userId", "user1");
        combinedFilter.put("eventType", AuditEventType.USER_ACTION);
        
        List<AuditLog> combinedLogs = auditService.getAuditLogs(combinedFilter);
        assertEquals(1, combinedLogs.size());
        assertEquals("LOGIN", combinedLogs.get(0).getAction());
    }
}