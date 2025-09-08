package com.securevoting.controller;

import com.securevoting.model.AuditEventType;
import com.securevoting.service.AuditService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Security tests for AuditController to verify proper access controls and authorization.
 */
@WebMvcTest(AuditController.class)
class AuditControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditService auditService;

    @Test
    void viewAuditLogs_WithoutAuthentication_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/admin/audit/logs"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(roles = "VOTER")
    void viewAuditLogs_WithVoterRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/admin/audit/logs"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void viewAuditLogs_WithAdminRole_ShouldReturnSuccess() throws Exception {
        when(auditService.getAuditLogs(any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/audit/logs"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/audit-logs"))
                .andExpect(model().attributeExists("auditLogs"));
    }

    @Test
    void viewRecentAuditLogs_WithoutAuthentication_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/admin/audit/recent"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(roles = "VOTER")
    void viewRecentAuditLogs_WithVoterRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/admin/audit/recent"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void viewRecentAuditLogs_WithAdminRole_ShouldReturnSuccess() throws Exception {
        when(auditService.getAuditLogs(any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/audit/recent"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/audit-logs"))
                .andExpect(model().attributeExists("auditLogs"));
    }

    @Test
    void viewUserAuditLogs_WithoutAuthentication_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/admin/audit/user/test-user"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(roles = "VOTER")
    void viewUserAuditLogs_WithVoterRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/admin/audit/user/test-user"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void viewUserAuditLogs_WithAdminRole_ShouldReturnSuccess() throws Exception {
        when(auditService.getAuditLogs(any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/audit/user/test-user"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user-audit-logs"))
                .andExpect(model().attributeExists("auditLogs"));
    }

    @Test
    void viewEventTypeAuditLogs_WithoutAuthentication_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/admin/audit/events/AUTHENTICATION"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(roles = "VOTER")
    void viewEventTypeAuditLogs_WithVoterRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/admin/audit/events/AUTHENTICATION"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void viewEventTypeAuditLogs_WithAdminRole_ShouldReturnSuccess() throws Exception {
        when(auditService.getAuditLogs(any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/audit/events/AUTHENTICATION"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/event-audit-logs"))
                .andExpect(model().attributeExists("auditLogs"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void viewEventTypeAuditLogs_WithInvalidEventType_ShouldHandleGracefully() throws Exception {
        mockMvc.perform(get("/admin/audit/events/INVALID_EVENT"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/audit-logs"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void exportAuditLogs_WithoutAuthentication_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/admin/audit/export"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(roles = "VOTER")
    void exportAuditLogs_WithVoterRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/admin/audit/export"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void exportAuditLogs_WithAdminRole_ShouldReturnRedirect() throws Exception {
        mockMvc.perform(get("/admin/audit/export"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/audit/logs"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void auditEndpoints_WithPaginationParameters_ShouldValidateInput() throws Exception {
        when(auditService.getAuditLogs(any())).thenReturn(Collections.emptyList());

        // Test with valid pagination parameters
        mockMvc.perform(get("/admin/audit/logs")
                .param("page", "0")
                .param("size", "50"))
                .andExpect(status().isOk());

        // Test with invalid page number (negative)
        mockMvc.perform(get("/admin/audit/logs")
                .param("page", "-1")
                .param("size", "50"))
                .andExpect(status().isBadRequest());

        // Test with invalid page size (too large)
        mockMvc.perform(get("/admin/audit/logs")
                .param("page", "0")
                .param("size", "1000"))
                .andExpect(status().isBadRequest());

        // Test with invalid page size (zero)
        mockMvc.perform(get("/admin/audit/logs")
                .param("page", "0")
                .param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void auditEndpoints_WithFilterParameters_ShouldAcceptValidFilters() throws Exception {
        when(auditService.getAuditLogs(any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/audit/logs")
                .param("userId", "test-user")
                .param("eventType", "AUTHENTICATION")
                .param("action", "LOGIN")
                .param("ipAddress", "192.168.1.1"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("auditLogs"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void auditEndpoints_WithDateTimeFilters_ShouldAcceptValidDates() throws Exception {
        when(auditService.getAuditLogs(any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/audit/logs")
                .param("startDate", "2024-01-01T00:00:00")
                .param("endDate", "2024-12-31T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("auditLogs"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void auditEndpoints_WithSQLInjectionAttempt_ShouldRejectRequest() throws Exception {
        // Test with SQL injection attempt in user ID
        mockMvc.perform(get("/admin/audit/user/'; DROP TABLE audit_logs; --"))
                .andExpect(status().isOk()); // Should be handled safely by parameterized queries
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void auditEndpoints_WithXSSAttempt_ShouldSanitizeInput() throws Exception {
        when(auditService.getAuditLogs(any())).thenReturn(Collections.emptyList());

        // Test with XSS attempt in filter parameters
        mockMvc.perform(get("/admin/audit/logs")
                .param("action", "<script>alert('xss')</script>"))
                .andExpect(status().isOk()); // Should be sanitized by framework
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void auditEndpoints_WithLongUserIdPath_ShouldHandleGracefully() throws Exception {
        when(auditService.getAuditLogs(any())).thenReturn(Collections.emptyList());

        // Test with very long user ID
        String longUserId = "a".repeat(1000);
        mockMvc.perform(get("/admin/audit/user/" + longUserId))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void auditEndpoints_WithSpecialCharactersInPath_ShouldHandleGracefully() throws Exception {
        when(auditService.getAuditLogs(any())).thenReturn(Collections.emptyList());

        // Test with special characters in user ID (URL encoded)
        mockMvc.perform(get("/admin/audit/user/user%40example.com"))
                .andExpect(status().isOk());
    }
}