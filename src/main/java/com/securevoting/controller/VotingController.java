package com.securevoting.controller;

import com.securevoting.model.Election;
import com.securevoting.service.VoteResult;
import com.securevoting.service.VotingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller handling voting operations including election display and vote submission.
 * Provides secure voting interface with proper validation and error handling.
 */
@Controller
@RequestMapping("/voting")
public class VotingController {
    
    private static final Logger logger = LoggerFactory.getLogger(VotingController.class);
    
    @Autowired
    private VotingService votingService;
    
    /**
     * Display available elections for voting.
     */
    @GetMapping("/elections")
    public String showElections(Model model, HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }
        
        try {
            List<Election> activeElections = votingService.getActiveElections();
            model.addAttribute("elections", activeElections);
            model.addAttribute("userId", userId);
            
            // Add voting status for each election
            for (Election election : activeElections) {
                boolean hasVoted = votingService.hasUserVoted(userId, election.getElectionId());
                model.addAttribute("hasVoted_" + election.getElectionId(), hasVoted);
            }
            
            return "voting/elections";
            
        } catch (Exception e) {
            logger.error("Failed to load elections for user: {}", userId, e);
            model.addAttribute("error", "Failed to load elections. Please try again.");
            return "voting/elections";
        }
    }
    
    /**
     * Display election details and voting interface.
     */
    @GetMapping("/election/{electionId}")
    public String showElectionDetails(@PathVariable("electionId") 
                                     @NotBlank(message = "Election ID is required") 
                                     @Pattern(regexp = "^[a-zA-Z0-9-]+$", message = "Invalid election ID format") 
                                     String electionId,
                                     Model model, 
                                     HttpSession session) {
        
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }
        
        try {
            // Check if election exists and is active
            if (!votingService.isElectionActive(electionId)) {
                model.addAttribute("error", "Election is not available for voting.");
                return "voting/elections";
            }
            
            // Check if user has already voted
            if (votingService.hasUserVoted(userId, electionId)) {
                model.addAttribute("message", "You have already voted in this election.");
                model.addAttribute("alreadyVoted", true);
            }
            
            Election election = votingService.getElectionDetails(electionId);
            if (election == null) {
                model.addAttribute("error", "Election not found.");
                return "voting/elections";
            }
            
            model.addAttribute("election", election);
            model.addAttribute("candidates", election.getCandidates());
            model.addAttribute("userId", userId);
            
            return "voting/election-details";
            
        } catch (Exception e) {
            logger.error("Failed to load election details for election: {} and user: {}", electionId, userId, e);
            model.addAttribute("error", "Failed to load election details. Please try again.");
            return "voting/elections";
        }
    }
    
    /**
     * Handle vote submission.
     */
    @PostMapping("/cast-vote")
    public String castVote(@RequestParam("candidateId") 
                          @NotBlank(message = "Candidate selection is required") 
                          @Pattern(regexp = "^[a-zA-Z0-9-]+$", message = "Invalid candidate ID format") 
                          String candidateId,
                          
                          @RequestParam("electionId") 
                          @NotBlank(message = "Election ID is required") 
                          @Pattern(regexp = "^[a-zA-Z0-9-]+$", message = "Invalid election ID format") 
                          String electionId,
                          
                          HttpSession session,
                          HttpServletRequest request,
                          RedirectAttributes redirectAttributes) {
        
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }
        
        try {
            // Validate election is active
            if (!votingService.isElectionActive(electionId)) {
                redirectAttributes.addFlashAttribute("error", "Election is not available for voting.");
                return "redirect:/voting/elections";
            }
            
            // Validate candidate exists in election
            if (!votingService.isCandidateValid(candidateId, electionId)) {
                redirectAttributes.addFlashAttribute("error", "Invalid candidate selection.");
                return "redirect:/voting/election/" + electionId;
            }
            
            // Check if user has already voted
            if (votingService.hasUserVoted(userId, electionId)) {
                redirectAttributes.addFlashAttribute("error", "You have already voted in this election.");
                return "redirect:/voting/election/" + electionId;
            }
            
            // Cast the vote
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            
            VoteResult result = votingService.castVote(userId, candidateId, electionId, ipAddress, userAgent);
            
            if (result.isSuccessful()) {
                logger.info("Vote cast successfully by user: {} in election: {}", userId, electionId);
                redirectAttributes.addFlashAttribute("message", "Your vote has been cast successfully!");
                redirectAttributes.addFlashAttribute("voteId", result.getVoteId());
                return "redirect:/voting/confirmation";
            } else {
                logger.warn("Vote casting failed for user: {} in election: {} - {}", userId, electionId, result.getMessage());
                redirectAttributes.addFlashAttribute("error", result.getMessage());
                return "redirect:/voting/election/" + electionId;
            }
            
        } catch (Exception e) {
            logger.error("Error casting vote for user: {} in election: {}", userId, electionId, e);
            redirectAttributes.addFlashAttribute("error", "An error occurred while casting your vote. Please try again.");
            return "redirect:/voting/election/" + electionId;
        }
    }
    
    /**
     * Display vote confirmation page.
     */
    @GetMapping("/confirmation")
    public String showVoteConfirmation(Model model, HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }
        
        model.addAttribute("userId", userId);
        return "voting/confirmation";
    }
    
    /**
     * Display user's voting history.
     */
    @GetMapping("/history")
    public String showVotingHistory(Model model, HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }
        
        try {
            // Get all elections and check which ones the user has voted in
            List<Election> allElections = votingService.getActiveElections();
            model.addAttribute("elections", allElections);
            model.addAttribute("userId", userId);
            
            // Add voting status for each election
            for (Election election : allElections) {
                boolean hasVoted = votingService.hasUserVoted(userId, election.getElectionId());
                model.addAttribute("hasVoted_" + election.getElectionId(), hasVoted);
                
                if (hasVoted) {
                    // Get vote details for verification (encrypted data only)
                    var vote = votingService.getUserVote(userId, election.getElectionId());
                    if (vote != null) {
                        model.addAttribute("voteTimestamp_" + election.getElectionId(), vote.getTimestamp());
                        model.addAttribute("voteId_" + election.getElectionId(), vote.getVoteId());
                    }
                }
            }
            
            return "voting/history";
            
        } catch (Exception e) {
            logger.error("Failed to load voting history for user: {}", userId, e);
            model.addAttribute("error", "Failed to load voting history. Please try again.");
            return "voting/history";
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