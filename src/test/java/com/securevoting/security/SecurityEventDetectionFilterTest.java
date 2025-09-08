package com.securevoting.security;

import com.securevoting.model.AuditEventType;
import com.securevoting.service.AuditService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Enumeration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityEventDetectionFilterTest {

    @Mock
    private AuditService auditService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private SecurityEventDetectionFilter securityEventDetectionFilter;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(securityEventDetectionFilter, "failedAttemptsThreshold", 3);
        ReflectionTestUtils.setField(securityEventDetectionFilter, "suspiciousPatternsEnabled", true);
    }

    @Test
    void testSuspiciousPattern_SQLInjection_LogsSecurityEvent() throws Exception {
        // Given
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(request.getRequestURI()).thenReturn("/login");
        when(request.getMethod()).thenReturn("POST");
        when(request.getQueryString()).thenReturn("username=admin' OR '1'='1");
        when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());

        // When
        securityEventDetectionFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(auditService).logSecurityEvent(
            eq(AuditEventType.SECURITY_VIOLATION),
            contains("Suspicious request pattern detected"),
            eq("192.168.1.1")
        );
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testSuspiciousPattern_XSSAttempt_LogsSecurityEvent() throws Exception {
        // Given
        when(request.getRemoteAddr()).thenReturn("192.168.1.2");
        when(request.getRequestURI()).thenReturn("/register");
        when(request.getMethod()).thenReturn("POST");
        when(request.getQueryString()).thenReturn("name=<script>alert('xss')</script>");
        when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());

        // When
        securityEventDetectionFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(auditService).logSecurityEvent(
            eq(AuditEventType.SECURITY_VIOLATION),
            contains("Suspicious request pattern detected"),
            eq("192.168.1.2")
        );
    }

    @Test
    void testSuspiciousUserAgent_AttackTool_LogsSecurityEvent() throws Exception {
        // Given
        when(request.getRemoteAddr()).thenReturn("192.168.1.3");
        when(request.getRequestURI()).thenReturn("/login");
        when(request.getMethod()).thenReturn("GET");
        when(request.getQueryString()).thenReturn(null);
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(Collections.singletonList("User-Agent")));
        when(request.getHeader("User-Agent")).thenReturn("sqlmap/1.0");

        // When
        securityEventDetectionFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(auditService).logSecurityEvent(
            eq(AuditEventType.SECURITY_VIOLATION),
            contains("Suspicious request pattern detected"),
            eq("192.168.1.3")
        );
    }

    @Test
    void testMultipleFailedLogins_ExceedsThreshold_LogsSecurityEvent() throws Exception {
        // Given
        String clientIp = "192.168.1.4";
        when(request.getRemoteAddr()).thenReturn(clientIp);
        when(request.getRequestURI()).thenReturn("/login");
        when(request.getMethod()).thenReturn("POST");
        when(request.getQueryString()).thenReturn(null);
        when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(response.getStatus()).thenReturn(401); // Unauthorized

        // When - simulate multiple failed attempts
        for (int i = 0; i < 3; i++) {
            securityEventDetectionFilter.doFilterInternal(request, response, filterChain);
        }

        // Then
        verify(auditService).logSecurityEvent(
            eq(AuditEventType.SECURITY_VIOLATION),
            contains("Multiple failed login attempts detected"),
            eq(clientIp)
        );
    }

    @Test
    void testSuccessfulLogin_ResetsFailedAttempts() throws Exception {
        // Given
        String clientIp = "192.168.1.5";
        when(request.getRemoteAddr()).thenReturn(clientIp);
        when(request.getRequestURI()).thenReturn("/login");
        when(request.getMethod()).thenReturn("POST");
        when(request.getQueryString()).thenReturn(null);
        when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());

        // Simulate failed attempts
        when(response.getStatus()).thenReturn(401);
        securityEventDetectionFilter.doFilterInternal(request, response, filterChain);
        securityEventDetectionFilter.doFilterInternal(request, response, filterChain);

        // When - successful login
        when(response.getStatus()).thenReturn(200);
        securityEventDetectionFilter.doFilterInternal(request, response, filterChain);

        // Then - should not log security event for failed attempts
        verify(auditService, never()).logSecurityEvent(
            eq(AuditEventType.SECURITY_VIOLATION),
            contains("Multiple failed login attempts detected"),
            eq(clientIp)
        );
    }

    @Test
    void testNormalRequest_NoSuspiciousActivity_NoSecurityEvent() throws Exception {
        // Given
        when(request.getRemoteAddr()).thenReturn("192.168.1.6");
        when(request.getRequestURI()).thenReturn("/dashboard");
        when(request.getMethod()).thenReturn("GET");
        when(request.getQueryString()).thenReturn(null);
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(Collections.singletonList("User-Agent")));
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        when(response.getStatus()).thenReturn(200);

        // When
        securityEventDetectionFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(auditService, never()).logSecurityEvent(any(), any(), any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testPathTraversalAttempt_LogsSecurityEvent() throws Exception {
        // Given
        when(request.getRemoteAddr()).thenReturn("192.168.1.7");
        when(request.getRequestURI()).thenReturn("/files");
        when(request.getMethod()).thenReturn("GET");
        when(request.getQueryString()).thenReturn("file=../../../etc/passwd");
        when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());

        // When
        securityEventDetectionFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(auditService).logSecurityEvent(
            eq(AuditEventType.SECURITY_VIOLATION),
            contains("Suspicious request pattern detected"),
            eq("192.168.1.7")
        );
    }

    @Test
    void testXForwardedForHeader_UsesCorrectIP() throws Exception {
        // Given
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 192.168.1.1");
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(request.getRequestURI()).thenReturn("/login");
        when(request.getMethod()).thenReturn("POST");
        when(request.getQueryString()).thenReturn("username=admin' OR '1'='1");
        when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());

        // When
        securityEventDetectionFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(auditService).logSecurityEvent(
            eq(AuditEventType.SECURITY_VIOLATION),
            contains("Suspicious request pattern detected"),
            eq("10.0.0.1") // Should use the first IP from X-Forwarded-For
        );
    }

    @Test
    void testCleanupOldEntries_RemovesExpiredData() {
        // When
        securityEventDetectionFilter.cleanupOldEntries();

        // Then - should complete without errors
        // This is mainly testing that the cleanup method doesn't throw exceptions
    }

    @Test
    void testFilterException_ContinuesFilterChain() throws Exception {
        // Given
        when(request.getRemoteAddr()).thenThrow(new RuntimeException("Test exception"));

        // When
        securityEventDetectionFilter.doFilterInternal(request, response, filterChain);

        // Then - should continue despite exception
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testSuspiciousPatternsDisabled_NoDetection() throws Exception {
        // Given
        ReflectionTestUtils.setField(securityEventDetectionFilter, "suspiciousPatternsEnabled", false);
        when(request.getRemoteAddr()).thenReturn("192.168.1.8");
        when(request.getRequestURI()).thenReturn("/login");
        when(request.getMethod()).thenReturn("POST");
        when(request.getQueryString()).thenReturn("username=admin' OR '1'='1");
        when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());

        // When
        securityEventDetectionFilter.doFilterInternal(request, response, filterChain);

        // Then - should not log security event when detection is disabled
        verify(auditService, never()).logSecurityEvent(any(), any(), any());
    }
}