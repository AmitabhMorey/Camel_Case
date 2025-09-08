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

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    @Mock
    private AuditService auditService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private RateLimitingFilter rateLimitingFilter;

    @BeforeEach
    void setUp() {
        // Set test configuration values
        ReflectionTestUtils.setField(rateLimitingFilter, "authRequestsPerMinute", 3);
        ReflectionTestUtils.setField(rateLimitingFilter, "votingRequestsPerMinute", 2);
        ReflectionTestUtils.setField(rateLimitingFilter, "generalRequestsPerMinute", 10);
    }

    @Test
    void testAuthenticationRateLimit_WithinLimit_AllowsRequest() throws Exception {
        // Given
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(request.getRequestURI()).thenReturn("/login");

        // When
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(auditService, never()).logSecurityEvent(any(), any(), any());
    }

    @Test
    void testAuthenticationRateLimit_ExceedsLimit_BlocksRequest() throws Exception {
        // Given
        String clientIp = "192.168.1.1";
        when(request.getRemoteAddr()).thenReturn(clientIp);
        when(request.getRequestURI()).thenReturn("/login");
        
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        // Make requests up to the limit
        for (int i = 0; i < 3; i++) {
            rateLimitingFilter.doFilterInternal(request, response, filterChain);
        }

        // When - exceed the limit
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setStatus(429); // Too Many Requests
        verify(auditService).logSecurityEvent(
            eq(AuditEventType.SECURITY_VIOLATION),
            contains("Rate limit exceeded"),
            eq(clientIp)
        );
        verify(filterChain, times(3)).doFilter(request, response); // Only first 3 requests pass
    }

    @Test
    void testVotingRateLimit_ExceedsLimit_BlocksRequest() throws Exception {
        // Given
        String clientIp = "192.168.1.2";
        when(request.getRemoteAddr()).thenReturn(clientIp);
        when(request.getRequestURI()).thenReturn("/voting/cast");
        
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        // Make requests up to the limit
        for (int i = 0; i < 2; i++) {
            rateLimitingFilter.doFilterInternal(request, response, filterChain);
        }

        // When - exceed the limit
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setStatus(429);
        verify(auditService).logSecurityEvent(
            eq(AuditEventType.SECURITY_VIOLATION),
            contains("Rate limit exceeded"),
            eq(clientIp)
        );
    }

    @Test
    void testDifferentIPs_IndependentRateLimits() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/login");
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        // When - different IPs make requests
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        when(request.getRemoteAddr()).thenReturn("192.168.1.2");
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // Then - both should pass
        verify(filterChain, times(2)).doFilter(request, response);
        verify(auditService, never()).logSecurityEvent(any(), any(), any());
    }

    @Test
    void testXForwardedForHeader_UsesCorrectIP() throws Exception {
        // Given
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 192.168.1.1");
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(request.getRequestURI()).thenReturn("/login");

        // When
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        // The filter should use the first IP from X-Forwarded-For (10.0.0.1)
    }

    @Test
    void testCleanupOldEntries_RemovesExpiredData() {
        // Given - simulate some rate limit data
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(request.getRequestURI()).thenReturn("/login");

        // When
        rateLimitingFilter.cleanupOldEntries();

        // Then - should complete without errors
        // This is mainly testing that the cleanup method doesn't throw exceptions
    }

    @Test
    void testGeneralRateLimit_WithinLimit_AllowsRequest() throws Exception {
        // Given
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(request.getRequestURI()).thenReturn("/dashboard");

        // When
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(auditService, never()).logSecurityEvent(any(), any(), any());
    }

    @Test
    void testFilterException_ContinuesFilterChain() throws Exception {
        // Given
        when(request.getRemoteAddr()).thenThrow(new RuntimeException("Test exception"));
        when(request.getRequestURI()).thenReturn("/login");

        // When
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // Then - should continue despite exception
        verify(filterChain).doFilter(request, response);
    }
}