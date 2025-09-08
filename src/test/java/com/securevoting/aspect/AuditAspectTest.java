package com.securevoting.aspect;

import com.securevoting.model.AuditEventType;
import com.securevoting.service.AuditService;
import com.securevoting.service.AuthenticationResult;
import com.securevoting.service.AuthenticationService;
import com.securevoting.service.VoteResult;
import com.securevoting.service.VotingService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuditAspect.
 */
@ExtendWith(MockitoExtension.class)
class AuditAspectTest {
    
    @Mock
    private AuditService auditService;
    
    @Mock
    private JoinPoint joinPoint;
    
    @Mock
    private Signature signature;
    
    @InjectMocks
    private AuditAspect auditAspect;
    
    private MockHttpServletRequest mockRequest;
    
    @BeforeEach
    void setUp() {
        mockRequest = new MockHttpServletRequest();
        mockRequest.setRemoteAddr("192.168.1.100");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequest));
        
        lenient().when(joinPoint.getSignature()).thenReturn(signature);
    }
    
    @Test
    void testLogAuthenticationSuccess() {
        // Arrange
        when(signature.getName()).thenReturn("authenticateWithQR");
        when(joinPoint.getArgs()).thenReturn(new Object[]{"test-user-123", "qr-data"});
        
        AuthenticationResult result = new AuthenticationResult();
        result.setSuccessful(true);
        
        // Act
        auditAspect.logAuthenticationSuccess(joinPoint, result);
        
        // Assert
        verify(auditService).logAuthenticationAttempt(
                eq("test-user-123"), 
                eq(true), 
                eq("QR_CODE"), 
                eq("192.168.1.100"));
    }
    
    @Test
    void testLogAuthenticationFailure() {
        // Arrange
        when(signature.getName()).thenReturn("authenticateWithOTP");
        when(joinPoint.getArgs()).thenReturn(new Object[]{"test-user-123", "invalid-otp"});
        
        Exception exception = new RuntimeException("Authentication failed");
        
        // Act
        auditAspect.logAuthenticationFailure(joinPoint, exception);
        
        // Assert
        verify(auditService).logAuthenticationAttempt(
                eq("test-user-123"), 
                eq(false), 
                eq("OTP"), 
                eq("192.168.1.100"));
    }
    
    @Test
    void testLogVoteCastSuccess() {
        // Arrange
        when(joinPoint.getArgs()).thenReturn(new Object[]{"test-user-123", "candidate-456", "election-789"});
        
        VoteResult result = VoteResult.success("vote-123");
        
        // Act
        auditAspect.logVoteCastSuccess(joinPoint, result);
        
        // Assert
        verify(auditService).logVotingEvent(
                eq("test-user-123"), 
                eq("election-789"), 
                eq(AuditEventType.VOTE_SUCCESS), 
                eq("192.168.1.100"));
    }
    
    @Test
    void testLogVoteCastFailure() {
        // Arrange
        when(joinPoint.getArgs()).thenReturn(new Object[]{"test-user-123", "candidate-456", "election-789"});
        
        Exception exception = new RuntimeException("Vote casting failed");
        
        // Act
        auditAspect.logVoteCastFailure(joinPoint, exception);
        
        // Assert
        verify(auditService).logVotingEvent(
                eq("test-user-123"), 
                eq("election-789"), 
                eq(AuditEventType.VOTE_FAILED), 
                eq("192.168.1.100"));
    }
    
    @Test
    void testLogAdminAction() {
        // Arrange
        lenient().when(signature.getName()).thenReturn("createElection");
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{"election-request-123"});
        
        // Act
        auditAspect.logAdminAction(joinPoint);
        
        // Assert - Admin action logging is currently disabled
        verify(auditService, never()).logAdminAction(anyString(), anyString(), anyString(), anyString());
    }
    
    @Test
    void testLogSecurityViolation() {
        // Arrange
        when(signature.getName()).thenReturn("sensitiveMethod");
        when(signature.getDeclaringType()).thenReturn((Class) AuthenticationService.class);
        
        Exception exception = new SecurityException("Access denied");
        
        // Act
        auditAspect.logSecurityViolation(joinPoint, exception);
        
        // Assert
        verify(auditService).logSecurityEvent(
                eq(AuditEventType.SECURITY_VIOLATION), 
                contains("Security violation in AuthenticationService.sensitiveMethod"), 
                eq("192.168.1.100"));
    }
    
    @Test
    void testLogAuthenticationSuccessWithXForwardedFor() {
        // Arrange
        mockRequest.addHeader("X-Forwarded-For", "203.0.113.1, 192.168.1.100");
        
        when(signature.getName()).thenReturn("authenticateWithQR");
        when(joinPoint.getArgs()).thenReturn(new Object[]{"test-user-123", "qr-data"});
        
        AuthenticationResult result = new AuthenticationResult();
        result.setSuccessful(true);
        
        // Act
        auditAspect.logAuthenticationSuccess(joinPoint, result);
        
        // Assert
        verify(auditService).logAuthenticationAttempt(
                eq("test-user-123"), 
                eq(true), 
                eq("QR_CODE"), 
                eq("203.0.113.1"));
    }
    
    @Test
    void testLogAuthenticationSuccessWithXRealIp() {
        // Arrange
        mockRequest.addHeader("X-Real-IP", "203.0.113.2");
        
        when(signature.getName()).thenReturn("authenticateWithOTP");
        when(joinPoint.getArgs()).thenReturn(new Object[]{"test-user-123", "123456"});
        
        AuthenticationResult result = new AuthenticationResult();
        result.setSuccessful(true);
        
        // Act
        auditAspect.logAuthenticationSuccess(joinPoint, result);
        
        // Assert
        verify(auditService).logAuthenticationAttempt(
                eq("test-user-123"), 
                eq(true), 
                eq("OTP"), 
                eq("203.0.113.2"));
    }
    
    @Test
    void testLogAuthenticationSuccessWithException() {
        // Arrange
        when(signature.getName()).thenReturn("authenticateWithQR");
        when(joinPoint.getArgs()).thenReturn(new Object[]{"test-user-123", "qr-data"});
        when(auditService.logAuthenticationAttempt(anyString(), anyBoolean(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Audit service failure"));
        
        AuthenticationResult result = new AuthenticationResult();
        result.setSuccessful(true);
        
        // Act - should not throw exception
        auditAspect.logAuthenticationSuccess(joinPoint, result);
        
        // Assert - verify the audit service was called despite the exception
        verify(auditService).logAuthenticationAttempt(anyString(), anyBoolean(), anyString(), anyString());
    }
    
    @Test
    void testExtractAuthenticationMethodUnknown() {
        // Arrange
        when(signature.getName()).thenReturn("someOtherMethod");
        when(joinPoint.getArgs()).thenReturn(new Object[]{"test-user-123"});
        
        AuthenticationResult result = new AuthenticationResult();
        result.setSuccessful(true);
        
        // Act
        auditAspect.logAuthenticationSuccess(joinPoint, result);
        
        // Assert
        verify(auditService).logAuthenticationAttempt(
                eq("test-user-123"), 
                eq(true), 
                eq("UNKNOWN"), 
                eq("192.168.1.100"));
    }
}