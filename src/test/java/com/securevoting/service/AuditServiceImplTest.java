package com.securevoting.service;

import com.securevoting.model.AuditEventType;
import com.securevoting.model.AuditLog;
import com.securevoting.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuditServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class AuditServiceImplTest {
    
    @Mock
    private AuditLogRepository auditLogRepository;
    
    @Mock
    private CryptographyService cryptographyService;
    
    @InjectMocks
    private AuditServiceImpl auditService;
    
    private AuditLog testAuditLog;
    private String testUserId;
    private String testIpAddress;
    
    @BeforeEach
    void setUp() {
        testUserId = "test-user-123";
        testIpAddress = "192.168.1.100";
        
        testAuditLog = new AuditLog();
        testAuditLog.setLogId("test-log-123");
        testAuditLog.setUserId(testUserId);
        testAuditLog.setAction("TEST_ACTION");
        testAuditLog.setDetails("Test details");
        testAuditLog.setIpAddress(testIpAddress);
        testAuditLog.setTimestamp(LocalDateTime.now());
        testAuditLog.setEventType(AuditEventType.USER_ACTION);
        testAuditLog.setIntegrityHash("test-hash");
    }
    
    @Test
    void testLogUserAction() {
        // Arrange
        String action = "LOGIN";
        String details = "User logged in successfully";
        String expectedHash = "generated-hash-123";
        
        when(cryptographyService.generateHash(anyString())).thenReturn(expectedHash);
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(testAuditLog);
        
        // Act
        AuditLog result = auditService.logUserAction(testUserId, action, details, testIpAddress);
        
        // Assert
        assertNotNull(result);
        verify(cryptographyService).generateHash(anyString());
        verify(auditLogRepository).save(any(AuditLog.class));
    }
    
    @Test
    void testLogSecurityEvent() {
        // Arrange
        AuditEventType eventType = AuditEventType.SECURITY_VIOLATION;
        String details = "Multiple failed login attempts detected";
        String expectedHash = "security-hash-123";
        
        when(cryptographyService.generateHash(anyString())).thenReturn(expectedHash);
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(testAuditLog);
        
        // Act
        AuditLog result = auditService.logSecurityEvent(eventType, details, testIpAddress);
        
        // Assert
        assertNotNull(result);
        verify(cryptographyService).generateHash(anyString());
        verify(auditLogRepository).save(any(AuditLog.class));
    }
    
    @Test
    void testLogVotingEvent() {
        // Arrange
        String electionId = "election-123";
        AuditEventType eventType = AuditEventType.VOTE_SUCCESS;
        String expectedHash = "voting-hash-123";
        
        when(cryptographyService.generateHash(anyString())).thenReturn(expectedHash);
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(testAuditLog);
        
        // Act
        AuditLog result = auditService.logVotingEvent(testUserId, electionId, eventType, testIpAddress);
        
        // Assert
        assertNotNull(result);
        verify(cryptographyService).generateHash(anyString());
        verify(auditLogRepository).save(any(AuditLog.class));
    }
    
    @Test
    void testLogAdminAction() {
        // Arrange
        String adminId = "admin-123";
        String action = "CREATE_ELECTION";
        String target = "election-456";
        String expectedHash = "admin-hash-123";
        
        when(cryptographyService.generateHash(anyString())).thenReturn(expectedHash);
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(testAuditLog);
        
        // Act
        AuditLog result = auditService.logAdminAction(adminId, action, target, testIpAddress);
        
        // Assert
        assertNotNull(result);
        verify(cryptographyService).generateHash(anyString());
        verify(auditLogRepository).save(any(AuditLog.class));
    }
    
    @Test
    void testLogAuthenticationAttemptSuccess() {
        // Arrange
        String method = "QR_CODE";
        boolean success = true;
        String expectedHash = "auth-hash-123";
        
        when(cryptographyService.generateHash(anyString())).thenReturn(expectedHash);
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(testAuditLog);
        
        // Act
        AuditLog result = auditService.logAuthenticationAttempt(testUserId, success, method, testIpAddress);
        
        // Assert
        assertNotNull(result);
        verify(cryptographyService).generateHash(anyString());
        verify(auditLogRepository).save(any(AuditLog.class));
    }
    
    @Test
    void testLogAuthenticationAttemptFailure() {
        // Arrange
        String method = "OTP";
        boolean success = false;
        String expectedHash = "auth-fail-hash-123";
        
        when(cryptographyService.generateHash(anyString())).thenReturn(expectedHash);
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(testAuditLog);
        
        // Act
        AuditLog result = auditService.logAuthenticationAttempt(testUserId, success, method, testIpAddress);
        
        // Assert
        assertNotNull(result);
        verify(cryptographyService).generateHash(anyString());
        verify(auditLogRepository).save(any(AuditLog.class));
    }
    
    @Test
    void testGetAuditLogsWithFilters() {
        // Arrange
        Map<String, Object> filters = new HashMap<>();
        filters.put("userId", testUserId);
        filters.put("eventType", AuditEventType.USER_ACTION);
        filters.put("startDate", LocalDateTime.now().minusDays(1));
        filters.put("endDate", LocalDateTime.now());
        filters.put("ipAddress", testIpAddress);
        
        List<AuditLog> expectedLogs = Arrays.asList(testAuditLog);
        when(auditLogRepository.findByFilters(anyString(), any(AuditEventType.class), 
                any(LocalDateTime.class), any(LocalDateTime.class), anyString()))
                .thenReturn(expectedLogs);
        
        // Act
        List<AuditLog> result = auditService.getAuditLogs(filters);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testAuditLog, result.get(0));
        verify(auditLogRepository).findByFilters(anyString(), any(AuditEventType.class), 
                any(LocalDateTime.class), any(LocalDateTime.class), anyString());
    }
    
    @Test
    void testGetAuditLogsByUser() {
        // Arrange
        List<AuditLog> expectedLogs = Arrays.asList(testAuditLog);
        when(auditLogRepository.findByUserId(testUserId)).thenReturn(expectedLogs);
        
        // Act
        List<AuditLog> result = auditService.getAuditLogsByUser(testUserId, null, null);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testAuditLog, result.get(0));
        verify(auditLogRepository).findByUserId(testUserId);
    }
    
    @Test
    void testGetAuditLogsByUserWithDateRange() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now();
        List<AuditLog> expectedLogs = Arrays.asList(testAuditLog);
        
        when(auditLogRepository.findByUserIdAndTimestampBetween(testUserId, startDate, endDate))
                .thenReturn(expectedLogs);
        
        // Act
        List<AuditLog> result = auditService.getAuditLogsByUser(testUserId, startDate, endDate);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testAuditLog, result.get(0));
        verify(auditLogRepository).findByUserIdAndTimestampBetween(testUserId, startDate, endDate);
    }
    
    @Test
    void testGetAuditLogsByEventType() {
        // Arrange
        AuditEventType eventType = AuditEventType.USER_ACTION;
        List<AuditLog> expectedLogs = Arrays.asList(testAuditLog);
        when(auditLogRepository.findByEventType(eventType)).thenReturn(expectedLogs);
        
        // Act
        List<AuditLog> result = auditService.getAuditLogsByEventType(eventType, null, null);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testAuditLog, result.get(0));
        verify(auditLogRepository).findByEventType(eventType);
    }
    
    @Test
    void testVerifyLogIntegrityValid() {
        // Arrange
        String logId = "test-log-123";
        String expectedHash = "valid-hash-123";
        testAuditLog.setIntegrityHash(expectedHash);
        
        when(auditLogRepository.findById(logId)).thenReturn(Optional.of(testAuditLog));
        when(cryptographyService.generateHash(anyString())).thenReturn(expectedHash);
        
        // Act
        boolean result = auditService.verifyLogIntegrity(logId);
        
        // Assert
        assertTrue(result);
        verify(auditLogRepository).findById(logId);
        verify(cryptographyService).generateHash(anyString());
    }
    
    @Test
    void testVerifyLogIntegrityInvalid() {
        // Arrange
        String logId = "test-log-123";
        String storedHash = "stored-hash-123";
        String calculatedHash = "different-hash-456";
        testAuditLog.setIntegrityHash(storedHash);
        
        when(auditLogRepository.findById(logId)).thenReturn(Optional.of(testAuditLog));
        when(cryptographyService.generateHash(anyString())).thenReturn(calculatedHash);
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(testAuditLog);
        
        // Act
        boolean result = auditService.verifyLogIntegrity(logId);
        
        // Assert
        assertFalse(result);
        verify(auditLogRepository).findById(logId);
        verify(cryptographyService, times(2)).generateHash(anyString()); // Once for verification, once for security event
        verify(auditLogRepository).save(any(AuditLog.class)); // For logging security event
    }
    
    @Test
    void testVerifyLogIntegrityLogNotFound() {
        // Arrange
        String logId = "non-existent-log";
        when(auditLogRepository.findById(logId)).thenReturn(Optional.empty());
        
        // Act
        boolean result = auditService.verifyLogIntegrity(logId);
        
        // Assert
        assertFalse(result);
        verify(auditLogRepository).findById(logId);
        verify(cryptographyService, never()).generateHash(anyString());
    }
    
    @Test
    void testGenerateLogHash() {
        // Arrange
        String expectedHash = "generated-hash-123";
        when(cryptographyService.generateHash(anyString())).thenReturn(expectedHash);
        
        // Act
        String result = auditService.generateLogHash(testAuditLog);
        
        // Assert
        assertEquals(expectedHash, result);
        verify(cryptographyService).generateHash(anyString());
    }
    
    @Test
    void testGenerateLogHashException() {
        // Arrange
        when(cryptographyService.generateHash(anyString())).thenThrow(new RuntimeException("Hash generation failed"));
        
        // Act & Assert
        assertThrows(RuntimeException.class, () -> auditService.generateLogHash(testAuditLog));
        verify(cryptographyService).generateHash(anyString());
    }
}