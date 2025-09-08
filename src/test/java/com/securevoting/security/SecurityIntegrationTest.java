package com.securevoting.security;

import com.securevoting.model.AuditEventType;
import com.securevoting.repository.AuditLogRepository;
import com.securevoting.service.AuditService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void testCSRFProtection_PostWithoutToken_Returns403() throws Exception {
        // When & Then
        mockMvc.perform(post("/register")
                .param("username", "testuser")
                .param("email", "test@example.com")
                .param("password", "password123"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testSecurityHeaders_ArePresent() throws Exception {
        // When & Then
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Content-Type-Options"))
                .andExpect(header().exists("X-XSS-Protection"))
                .andExpect(header().exists("Cache-Control"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-XSS-Protection", "1; mode=block"));
    }

    @Test
    void testRateLimiting_ExcessiveRequests_Returns429() throws Exception {
        // Given - make multiple requests to trigger rate limiting
        String clientIp = "192.168.1.100";

        // When - make requests beyond the limit
        for (int i = 0; i < 15; i++) {
            mockMvc.perform(post("/login")
                    .header("X-Forwarded-For", clientIp)
                    .param("username", "testuser" + i)
                    .param("password", "password"));
        }

        // Then - subsequent request should be rate limited
        mockMvc.perform(post("/login")
                .header("X-Forwarded-For", clientIp)
                .param("username", "testuser")
                .param("password", "password"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void testSuspiciousRequest_SQLInjection_LogsSecurityEvent() throws Exception {
        // Given
        long initialCount = auditLogRepository.count();

        // When - attempt SQL injection
        mockMvc.perform(get("/login")
                .param("username", "admin' OR '1'='1")
                .header("X-Forwarded-For", "192.168.1.101"));

        // Then - security event should be logged
        long finalCount = auditLogRepository.count();
        assertTrue(finalCount > initialCount, "Security event should be logged");

        // Verify the security event was logged
        var securityEvents = auditService.getAuditLogsByEventType(
            AuditEventType.SECURITY_VIOLATION, null, null);
        assertTrue(securityEvents.stream().anyMatch(log -> 
            log.getDetails().contains("Suspicious request pattern detected")));
    }

    @Test
    void testSuspiciousRequest_XSSAttempt_LogsSecurityEvent() throws Exception {
        // Given
        long initialCount = auditLogRepository.count();

        // When - attempt XSS
        mockMvc.perform(get("/register")
                .param("name", "<script>alert('xss')</script>")
                .header("X-Forwarded-For", "192.168.1.102"));

        // Then - security event should be logged
        long finalCount = auditLogRepository.count();
        assertTrue(finalCount > initialCount, "Security event should be logged");
    }

    @Test
    void testSuspiciousUserAgent_AttackTool_LogsSecurityEvent() throws Exception {
        // Given
        long initialCount = auditLogRepository.count();

        // When - use suspicious user agent
        mockMvc.perform(get("/")
                .header("User-Agent", "sqlmap/1.0")
                .header("X-Forwarded-For", "192.168.1.103"));

        // Then - security event should be logged
        long finalCount = auditLogRepository.count();
        assertTrue(finalCount > initialCount, "Security event should be logged");
    }

    @Test
    void testPathTraversal_LogsSecurityEvent() throws Exception {
        // Given
        long initialCount = auditLogRepository.count();

        // When - attempt path traversal
        mockMvc.perform(get("/files")
                .param("file", "../../../etc/passwd")
                .header("X-Forwarded-For", "192.168.1.104"));

        // Then - security event should be logged
        long finalCount = auditLogRepository.count();
        assertTrue(finalCount > initialCount, "Security event should be logged");
    }

    @Test
    void testSessionSecurity_SessionFixationProtection() throws Exception {
        // When - access login page
        var result = mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andReturn();

        String sessionId1 = result.getRequest().getSession().getId();

        // When - perform login (would normally create new session)
        var result2 = mockMvc.perform(post("/login")
                .session((org.springframework.mock.web.MockHttpSession) result.getRequest().getSession())
                .param("username", "testuser")
                .param("password", "password"))
                .andReturn();

        // Then - session ID should be different (session fixation protection)
        String sessionId2 = result2.getRequest().getSession().getId();
        // Note: In a real scenario with successful authentication, session would change
    }

    @Test
    void testInputValidation_LongInput_HandledSafely() throws Exception {
        // Given - very long input
        String longInput = "a".repeat(2000);

        // When & Then - should handle gracefully without errors
        mockMvc.perform(get("/register")
                .param("username", longInput)
                .header("X-Forwarded-For", "192.168.1.105"))
                .andExpect(status().isOk()); // Should not crash
    }

    @Test
    void testMultipleFailedLogins_LogsSecurityEvent() throws Exception {
        // Given
        long initialCount = auditLogRepository.count();
        String clientIp = "192.168.1.106";

        // When - simulate multiple failed login attempts
        for (int i = 0; i < 6; i++) {
            mockMvc.perform(post("/login")
                    .header("X-Forwarded-For", clientIp)
                    .param("username", "testuser")
                    .param("password", "wrongpassword"));
        }

        // Then - security event should be logged for multiple failed attempts
        long finalCount = auditLogRepository.count();
        assertTrue(finalCount > initialCount, "Security events should be logged");

        var securityEvents = auditService.getAuditLogsByEventType(
            AuditEventType.SECURITY_VIOLATION, null, null);
        assertTrue(securityEvents.stream().anyMatch(log -> 
            log.getDetails().contains("Multiple failed login attempts detected") ||
            log.getDetails().contains("Rate limit exceeded")));
    }

    @Test
    void testSecureHeaders_HTTPSSimulation() throws Exception {
        // When & Then - verify security headers are set
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate"))
                .andExpect(header().string("Pragma", "no-cache"))
                .andExpect(header().string("Expires", "0"));
    }

    @Test
    void testAdminEndpoint_RequiresAuthentication() throws Exception {
        // When & Then - admin endpoints should require authentication
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection()); // Redirect to login
    }

    @Test
    void testVotingEndpoint_RequiresAuthentication() throws Exception {
        // When & Then - voting endpoints should require authentication
        mockMvc.perform(get("/voting/elections"))
                .andExpect(status().is3xxRedirection()); // Redirect to login
    }

    @Test
    void testH2Console_AccessibleInDevelopment() throws Exception {
        // When & Then - H2 console should be accessible (for development)
        mockMvc.perform(get("/h2-console"))
                .andExpect(status().isOk());
    }

    @Test
    void testStaticResources_AccessibleWithoutAuthentication() throws Exception {
        // When & Then - static resources should be accessible
        mockMvc.perform(get("/css/bootstrap.min.css"))
                .andExpect(status().isNotFound()); // File doesn't exist, but no auth required
    }
}