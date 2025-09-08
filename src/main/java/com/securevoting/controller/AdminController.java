package com.securevoting.controller;

import com.securevoting.model.Election;
import com.securevoting.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller handling administrative operations including election management,
 * result viewing, and system statistics. Secured with admin-only access controls.
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    
    @Autowired
    private AdminService adminService;
    
    @Autowired
    private AuditService auditService;
    
    /**
     * Display admin dashboard with system statistics.
     */
    @GetMapping("/dashboard")
    public String adminDashboard(Model model, HttpSession session, HttpServletRequest request) {
        String adminId = (String) session.getAttribute("userId");
        
        try {
            SystemStatistics stats = adminService.getSystemStatistics();
            model.addAttribute("statistics", stats);
            
            // Log admin dashboard access
            auditService.logAdminAction(adminId, "DASHBOARD_ACCESS", "Admin dashboard viewed", 
                getClientIpAddress(request));
            
            return "admin/dashboard";
            
        } catch (Exception e) {
            logger.error("Failed to load admin dashboard for admin: {}", adminId, e);
            model.addAttribute("error", "Failed to load dashboard statistics. Please try again.");
            return "admin/dashboard";
        }
    }
    
    /**
     * Display election management page.
     */
    @GetMapping("/elections")
    public String manageElections(@RequestParam(value = "status", required = false) String statusFilter,
                                 Model model, HttpSession session, HttpServletRequest request) {
        String adminId = (String) session.getAttribute("userId");
        
        try {
            List<Election> elections = adminService.getAllElections(statusFilter);
            model.addAttribute("elections", elections);
            model.addAttribute("statusFilter", statusFilter);
            
            // Log election management access
            auditService.logAdminAction(adminId, "ELECTION_MANAGEMENT_ACCESS", 
                "Election management page viewed with filter: " + statusFilter, 
                getClientIpAddress(request));
            
            return "admin/elections";
            
        } catch (Exception e) {
            logger.error("Failed to load elections for admin: {}", adminId, e);
            model.addAttribute("error", "Failed to load elections. Please try again.");
            return "admin/elections";
        }
    }
    
    /**
     * Display create election form.
     */
    @GetMapping("/elections/create")
    public String createElectionForm(Model model) {
        model.addAttribute("electionRequest", new ElectionRequest());
        return "admin/create-election";
    }
    
    /**
     * Handle election creation.
     */
    @PostMapping("/elections/create")
    public String createElection(@Valid @ModelAttribute("electionRequest") ElectionRequest electionRequest,
                                BindingResult result,
                                HttpSession session,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        
        String adminId = (String) session.getAttribute("userId");
        
        if (result.hasErrors()) {
            return "admin/create-election";
        }
        
        // Additional validation
        if (!electionRequest.isValid()) {
            result.rejectValue("title", "error.election", "Invalid election data provided");
            return "admin/create-election";
        }
        
        try {
            Election election = adminService.createElection(electionRequest, adminId);
            
            logger.info("Election created successfully by admin: {} - Election ID: {}", adminId, election.getElectionId());
            
            // Log election creation
            auditService.logAdminAction(adminId, "ELECTION_CREATED", 
                "Election created: " + election.getTitle() + " (ID: " + election.getElectionId() + ")", 
                getClientIpAddress(request));
            
            redirectAttributes.addFlashAttribute("message", 
                "Election '" + election.getTitle() + "' created successfully!");
            return "redirect:/admin/elections";
            
        } catch (IllegalArgumentException e) {
            result.rejectValue("title", "error.election", e.getMessage());
            return "admin/create-election";
        } catch (Exception e) {
            logger.error("Failed to create election for admin: {}", adminId, e);
            result.rejectValue("title", "error.election", "Failed to create election. Please try again.");
            return "admin/create-election";
        }
    }
    
    /**
     * Display election details and management options.
     */
    @GetMapping("/elections/{electionId}")
    public String viewElectionDetails(@PathVariable("electionId") 
                                     @NotBlank(message = "Election ID is required") 
                                     @Pattern(regexp = "^[a-zA-Z0-9-]+$", message = "Invalid election ID format") 
                                     String electionId,
                                     Model model, 
                                     HttpSession session, 
                                     HttpServletRequest request) {
        
        String adminId = (String) session.getAttribute("userId");
        
        try {
            List<Election> elections = adminService.getAllElections(null);
            Election election = elections.stream()
                .filter(e -> e.getElectionId().equals(electionId))
                .findFirst()
                .orElse(null);
            
            if (election == null) {
                model.addAttribute("error", "Election not found.");
                return "redirect:/admin/elections";
            }
            
            model.addAttribute("election", election);
            model.addAttribute("candidates", election.getCandidates());
            
            // Check for integrity issues
            List<String> integrityIssues = adminService.validateElectionIntegrity(electionId);
            model.addAttribute("integrityIssues", integrityIssues);
            
            // Log election details access
            auditService.logAdminAction(adminId, "ELECTION_DETAILS_ACCESS", 
                "Election details viewed: " + election.getTitle() + " (ID: " + electionId + ")", 
                getClientIpAddress(request));
            
            return "admin/election-details";
            
        } catch (Exception e) {
            logger.error("Failed to load election details for election: {} and admin: {}", electionId, adminId, e);
            model.addAttribute("error", "Failed to load election details. Please try again.");
            return "redirect:/admin/elections";
        }
    }    
    /*
*
     * Activate an election.
     */
    @PostMapping("/elections/{electionId}/activate")
    public String activateElection(@PathVariable("electionId") 
                                  @NotBlank(message = "Election ID is required") 
                                  @Pattern(regexp = "^[a-zA-Z0-9-]+$", message = "Invalid election ID format") 
                                  String electionId,
                                  HttpSession session,
                                  HttpServletRequest request,
                                  RedirectAttributes redirectAttributes) {
        
        String adminId = (String) session.getAttribute("userId");
        
        try {
            Election election = adminService.activateElection(electionId, adminId);
            
            logger.info("Election activated by admin: {} - Election ID: {}", adminId, electionId);
            
            // Log election activation
            auditService.logAdminAction(adminId, "ELECTION_ACTIVATED", 
                "Election activated: " + election.getTitle() + " (ID: " + electionId + ")", 
                getClientIpAddress(request));
            
            redirectAttributes.addFlashAttribute("message", 
                "Election '" + election.getTitle() + "' activated successfully!");
            
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to activate election {} by admin {}: {}", electionId, adminId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            logger.error("Error activating election {} by admin {}", electionId, adminId, e);
            redirectAttributes.addFlashAttribute("error", "Failed to activate election. Please try again.");
        }
        
        return "redirect:/admin/elections/" + electionId;
    }
    
    /**
     * Complete an election.
     */
    @PostMapping("/elections/{electionId}/complete")
    public String completeElection(@PathVariable("electionId") 
                                  @NotBlank(message = "Election ID is required") 
                                  @Pattern(regexp = "^[a-zA-Z0-9-]+$", message = "Invalid election ID format") 
                                  String electionId,
                                  HttpSession session,
                                  HttpServletRequest request,
                                  RedirectAttributes redirectAttributes) {
        
        String adminId = (String) session.getAttribute("userId");
        
        try {
            Election election = adminService.completeElection(electionId, adminId);
            
            logger.info("Election completed by admin: {} - Election ID: {}", adminId, electionId);
            
            // Log election completion
            auditService.logAdminAction(adminId, "ELECTION_COMPLETED", 
                "Election completed: " + election.getTitle() + " (ID: " + electionId + ")", 
                getClientIpAddress(request));
            
            redirectAttributes.addFlashAttribute("message", 
                "Election '" + election.getTitle() + "' completed successfully!");
            
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to complete election {} by admin {}: {}", electionId, adminId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            logger.error("Error completing election {} by admin {}", electionId, adminId, e);
            redirectAttributes.addFlashAttribute("error", "Failed to complete election. Please try again.");
        }
        
        return "redirect:/admin/elections/" + electionId;
    }
    
    /**
     * Delete an election.
     */
    @PostMapping("/elections/{electionId}/delete")
    public String deleteElection(@PathVariable("electionId") 
                                @NotBlank(message = "Election ID is required") 
                                @Pattern(regexp = "^[a-zA-Z0-9-]+$", message = "Invalid election ID format") 
                                String electionId,
                                HttpSession session,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        
        String adminId = (String) session.getAttribute("userId");
        
        try {
            adminService.deleteElection(electionId, adminId);
            
            logger.info("Election deleted by admin: {} - Election ID: {}", adminId, electionId);
            
            // Log election deletion
            auditService.logAdminAction(adminId, "ELECTION_DELETED", 
                "Election deleted (ID: " + electionId + ")", 
                getClientIpAddress(request));
            
            redirectAttributes.addFlashAttribute("message", "Election deleted successfully!");
            
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to delete election {} by admin {}: {}", electionId, adminId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            logger.error("Error deleting election {} by admin {}", electionId, adminId, e);
            redirectAttributes.addFlashAttribute("error", "Failed to delete election. Please try again.");
        }
        
        return "redirect:/admin/elections";
    }
    
    /**
     * Tally votes for an election and display results.
     */
    @GetMapping("/elections/{electionId}/results")
    public String viewElectionResults(@PathVariable("electionId") 
                                     @NotBlank(message = "Election ID is required") 
                                     @Pattern(regexp = "^[a-zA-Z0-9-]+$", message = "Invalid election ID format") 
                                     String electionId,
                                     Model model, 
                                     HttpSession session, 
                                     HttpServletRequest request) {
        
        String adminId = (String) session.getAttribute("userId");
        
        try {
            ElectionResult result = adminService.tallyVotes(electionId);
            model.addAttribute("electionResult", result);
            
            // Log results viewing
            auditService.logAdminAction(adminId, "ELECTION_RESULTS_VIEWED", 
                "Election results viewed: " + result.getElectionTitle() + " (ID: " + electionId + ")", 
                getClientIpAddress(request));
            
            return "admin/election-results";
            
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to tally votes for election {} by admin {}: {}", electionId, adminId, e.getMessage());
            model.addAttribute("error", e.getMessage());
            return "redirect:/admin/elections/" + electionId;
        } catch (Exception e) {
            logger.error("Error tallying votes for election {} by admin {}", electionId, adminId, e);
            model.addAttribute("error", "Failed to tally votes. Please try again.");
            return "redirect:/admin/elections/" + electionId;
        }
    }
    
    /**
     * Extract client IP address from request, handling proxy headers.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}