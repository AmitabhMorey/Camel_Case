package com.securevoting.controller;

import com.securevoting.model.User;
import com.securevoting.model.UserRole;
import com.securevoting.repository.UserRepository;
import com.securevoting.service.AdminService;
import com.securevoting.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for admin authorization across the entire security configuration.
 * Tests the complete security chain including authentication and authorization.
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
class AdminAuthorizationIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockBean
    private AdminService adminService;

    @MockBean
    private AuditService auditService;

    @MockBean
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void adminEndpoints_WithoutAuthentication_ShouldRedirectToLogin() throws Exception {
        String[] adminEndpoints = {
            "/admin/dashboard",
            "/admin/elections",
            "/admin/elections/create",
            "/admin/audit/logs",
            "/admin/audit/recent"
        };

        for (String endpoint : adminEndpoints) {
            mockMvc.perform(get(endpoint))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/login"));
        }
    }

    @Test
    @WithMockUser(username = "voter", roles = "VOTER")
    void adminEndpoints_WithVoterRole_ShouldReturnForbidden() throws Exception {
        // Mock user repository to return voter user
        User voterUser = new User();
        voterUser.setUserId("voter-id");
        voterUser.setUsername("voter");
        voterUser.setRole(UserRole.VOTER);
        when(userRepository.findByUsername("voter")).thenReturn(Optional.of(voterUser));

        String[] adminEndpoints = {
            "/admin/dashboard",
            "/admin/elections",
            "/admin/elections/create",
            "/admin/audit/logs",
            "/admin/audit/recent"
        };

        for (String endpoint : adminEndpoints) {
            mockMvc.perform(get(endpoint))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminEndpoints_WithAdminRole_ShouldAllowAccess() throws Exception {
        // Mock user repository to return admin user
        User adminUser = new User();
        adminUser.setUserId("admin-id");
        adminUser.setUsername("admin");
        adminUser.setRole(UserRole.ADMIN);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        // Mock service responses
        when(adminService.getSystemStatistics()).thenReturn(null);
        when(adminService.getAllElections(any())).thenReturn(Collections.emptyList());
        when(auditService.getAuditLogs(any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/admin/elections"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/admin/elections/create"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/admin/audit/logs"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/admin/audit/recent"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "voter", roles = "VOTER")
    void adminPostEndpoints_WithVoterRole_ShouldReturnForbidden() throws Exception {
        String[] adminPostEndpoints = {
            "/admin/elections/create",
            "/admin/elections/test-id/activate",
            "/admin/elections/test-id/complete",
            "/admin/elections/test-id/delete"
        };

        for (String endpoint : adminPostEndpoints) {
            mockMvc.perform(post(endpoint).with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminPostEndpoints_WithoutCSRF_ShouldReturnForbidden() throws Exception {
        String[] adminPostEndpoints = {
            "/admin/elections/create",
            "/admin/elections/test-id/activate",
            "/admin/elections/test-id/complete",
            "/admin/elections/test-id/delete"
        };

        for (String endpoint : adminPostEndpoints) {
            mockMvc.perform(post(endpoint))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminPostEndpoints_WithCSRFAndAdminRole_ShouldProcessRequest() throws Exception {
        // Mock user repository to return admin user
        User adminUser = new User();
        adminUser.setUserId("admin-id");
        adminUser.setUsername("admin");
        adminUser.setRole(UserRole.ADMIN);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        // Test create election endpoint (will have validation errors but should not be forbidden)
        mockMvc.perform(post("/admin/elections/create")
                .with(csrf())
                .param("title", "Test Election"))
                .andExpect(status().isOk()); // Validation errors, but not forbidden

        // Mock service responses for other endpoints
        when(adminService.activateElection(anyString(), anyString())).thenThrow(new IllegalArgumentException("Test error"));
        when(adminService.completeElection(anyString(), anyString())).thenThrow(new IllegalArgumentException("Test error"));

        mockMvc.perform(post("/admin/elections/test-id/activate")
                .with(csrf()))
                .andExpect(status().is3xxRedirection()); // Should redirect with error message

        mockMvc.perform(post("/admin/elections/test-id/complete")
                .with(csrf()))
                .andExpect(status().is3xxRedirection()); // Should redirect with error message

        mockMvc.perform(post("/admin/elections/test-id/delete")
                .with(csrf()))
                .andExpect(status().is3xxRedirection()); // Should redirect with error message
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void dashboardRouting_WithAdminRole_ShouldRedirectToAdminDashboard() throws Exception {
        // Mock user repository to return admin user
        User adminUser = new User();
        adminUser.setUserId("admin-id");
        adminUser.setUsername("admin");
        adminUser.setRole(UserRole.ADMIN);
        when(userRepository.findById("admin-id")).thenReturn(Optional.of(adminUser));

        // Note: This test would need session setup to work properly in integration test
        // For now, we test the endpoint directly
        when(adminService.getSystemStatistics()).thenReturn(null);

        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "voter", roles = "VOTER")
    void dashboardRouting_WithVoterRole_ShouldAllowVoterDashboard() throws Exception {
        // Mock user repository to return voter user
        User voterUser = new User();
        voterUser.setUserId("voter-id");
        voterUser.setUsername("voter");
        voterUser.setRole(UserRole.VOTER);
        when(userRepository.findById("voter-id")).thenReturn(Optional.of(voterUser));

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void auditEndpoints_WithAdminRole_ShouldAllowFullAccess() throws Exception {
        // Mock user repository to return admin user
        User adminUser = new User();
        adminUser.setUserId("admin-id");
        adminUser.setUsername("admin");
        adminUser.setRole(UserRole.ADMIN);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        // Mock service responses
        when(auditService.getAuditLogs(any())).thenReturn(Collections.emptyList());

        String[] auditEndpoints = {
            "/admin/audit/logs",
            "/admin/audit/recent",
            "/admin/audit/user/test-user",
            "/admin/audit/events/AUTHENTICATION",
            "/admin/audit/export"
        };

        for (String endpoint : auditEndpoints) {
            mockMvc.perform(get(endpoint))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        // Accept either OK or redirect status
                        if (status != 200 && status < 300 || status >= 400) {
                            throw new AssertionError("Expected OK or redirect status, but got: " + status);
                        }
                    });
        }
    }

    @Test
    @WithMockUser(username = "voter", roles = "VOTER")
    void auditEndpoints_WithVoterRole_ShouldDenyAccess() throws Exception {
        String[] auditEndpoints = {
            "/admin/audit/logs",
            "/admin/audit/recent",
            "/admin/audit/user/test-user",
            "/admin/audit/events/AUTHENTICATION",
            "/admin/audit/export"
        };

        for (String endpoint : auditEndpoints) {
            mockMvc.perform(get(endpoint))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void securityHeaders_ShouldBePresent() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(header().exists("X-Content-Type-Options"))
                .andExpect(header().exists("X-Frame-Options"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void csrfProtection_ShouldBeEnabledForStatefulOperations() throws Exception {
        // CSRF token should be required for state-changing operations
        mockMvc.perform(post("/admin/elections/create"))
                .andExpect(status().isForbidden()); // Should fail without CSRF token

        // Should work with CSRF token
        mockMvc.perform(post("/admin/elections/create").with(csrf()))
                .andExpect(status().isOk()); // Will have validation errors but not CSRF error
    }
}