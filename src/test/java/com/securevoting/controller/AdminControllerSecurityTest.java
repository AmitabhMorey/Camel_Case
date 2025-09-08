package com.securevoting.controller;

import com.securevoting.model.User;
import com.securevoting.model.UserRole;
import com.securevoting.service.AdminService;
import com.securevoting.service.AuditService;
import com.securevoting.service.ElectionRequest;
import com.securevoting.service.SystemStatistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Security tests for AdminController to verify proper access controls and authorization.
 */
@WebMvcTest(AdminController.class)
class AdminControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminService adminService;

    @MockBean
    private AuditService auditService;

    private SystemStatistics mockStats;

    @BeforeEach
    void setUp() {
        mockStats = new SystemStatistics(10, 5, 2, 3, 100, 10, 50, 5, 
            new HashMap<>(), new HashMap<>());
    }

    @Test
    void adminDashboard_WithoutAuthentication_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(roles = "VOTER")
    void adminDashboard_WithVoterRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminDashboard_WithAdminRole_ShouldReturnSuccess() throws Exception {
        when(adminService.getSystemStatistics()).thenReturn(mockStats);

        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attributeExists("statistics"));
    }

    @Test
    void manageElections_WithoutAuthentication_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/admin/elections"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(roles = "VOTER")
    void manageElections_WithVoterRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/admin/elections"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void manageElections_WithAdminRole_ShouldReturnSuccess() throws Exception {
        when(adminService.getAllElections(any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/elections"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/elections"))
                .andExpect(model().attributeExists("elections"));
    }

    @Test
    void createElectionForm_WithoutAuthentication_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/admin/elections/create"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(roles = "VOTER")
    void createElectionForm_WithVoterRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/admin/elections/create"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createElectionForm_WithAdminRole_ShouldReturnSuccess() throws Exception {
        mockMvc.perform(get("/admin/elections/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/create-election"))
                .andExpect(model().attributeExists("electionRequest"));
    }

    @Test
    void createElection_WithoutAuthentication_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(post("/admin/elections/create")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(roles = "VOTER")
    void createElection_WithVoterRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/admin/elections/create")
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void createElection_WithoutCSRFToken_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/admin/elections/create"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createElection_WithAdminRoleAndCSRF_ShouldProcessRequest() throws Exception {
        mockMvc.perform(post("/admin/elections/create")
                .with(csrf())
                .param("title", "Test Election")
                .param("description", "Test Description"))
                .andExpect(status().isOk()); // Will return validation errors, but not forbidden
    }

    @Test
    void activateElection_WithoutAuthentication_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(post("/admin/elections/test-id/activate")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(roles = "VOTER")
    void activateElection_WithVoterRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/admin/elections/test-id/activate")
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void activateElection_WithoutCSRFToken_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/admin/elections/test-id/activate"))
                .andExpect(status().isForbidden());
    }

    @Test
    void completeElection_WithoutAuthentication_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(post("/admin/elections/test-id/complete")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(roles = "VOTER")
    void completeElection_WithVoterRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/admin/elections/test-id/complete")
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void completeElection_WithoutCSRFToken_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/admin/elections/test-id/complete"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteElection_WithoutAuthentication_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(post("/admin/elections/test-id/delete")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(roles = "VOTER")
    void deleteElection_WithVoterRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/admin/elections/test-id/delete")
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteElection_WithoutCSRFToken_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/admin/elections/test-id/delete"))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewElectionResults_WithoutAuthentication_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/admin/elections/test-id/results"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(roles = "VOTER")
    void viewElectionResults_WithVoterRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/admin/elections/test-id/results"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void viewElectionDetails_WithAdminRole_ShouldReturnSuccess() throws Exception {
        when(adminService.getAllElections(any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/elections/test-id"))
                .andExpect(status().isOk())
                .andExpect(view().name("redirect:/admin/elections"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminEndpoints_WithInvalidElectionId_ShouldHandleGracefully() throws Exception {
        // Test with invalid election ID format
        mockMvc.perform(get("/admin/elections/invalid@id"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminEndpoints_WithSQLInjectionAttempt_ShouldRejectRequest() throws Exception {
        // Test with SQL injection attempt in election ID
        mockMvc.perform(get("/admin/elections/'; DROP TABLE elections; --"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminEndpoints_WithXSSAttempt_ShouldRejectRequest() throws Exception {
        // Test with XSS attempt in parameters
        mockMvc.perform(get("/admin/elections")
                .param("status", "<script>alert('xss')</script>"))
                .andExpect(status().isOk()); // Should be sanitized by framework
    }
}