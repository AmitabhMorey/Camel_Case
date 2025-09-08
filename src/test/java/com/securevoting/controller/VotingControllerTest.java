package com.securevoting.controller;

import com.securevoting.model.Candidate;
import com.securevoting.model.Election;
import com.securevoting.model.ElectionStatus;
import com.securevoting.model.Vote;
import com.securevoting.service.VoteResult;
import com.securevoting.service.VotingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for VotingController.
 */
@WebMvcTest(VotingController.class)
@Import({com.securevoting.config.TestSecurityConfig.class})
class VotingControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private VotingService votingService;
    
    private MockHttpSession session;
    private String testUserId;
    private String testElectionId;
    private String testCandidateId;
    private Election testElection;
    private Candidate testCandidate;
    
    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID().toString();
        testElectionId = UUID.randomUUID().toString();
        testCandidateId = UUID.randomUUID().toString();
        
        // Create mock session with authenticated user
        session = new MockHttpSession();
        session.setAttribute("userId", testUserId);
        session.setAttribute("authenticated", true);
        
        // Create test election
        testElection = new Election();
        testElection.setElectionId(testElectionId);
        testElection.setTitle("Test Election");
        testElection.setDescription("Test Description");
        testElection.setStatus(ElectionStatus.ACTIVE);
        testElection.setStartTime(LocalDateTime.now().minusHours(1));
        testElection.setEndTime(LocalDateTime.now().plusHours(1));
        
        // Create test candidate
        testCandidate = new Candidate();
        testCandidate.setCandidateId(testCandidateId);
        testCandidate.setName("Test Candidate");
        testCandidate.setDescription("Test Candidate Description");
        testCandidate.setElectionId(testElectionId);
        
        testElection.setCandidates(Arrays.asList(testCandidate));
    }
    
    @Test
    void testShowElections_Success() throws Exception {
        // Arrange
        List<Election> activeElections = Arrays.asList(testElection);
        when(votingService.getActiveElections()).thenReturn(activeElections);
        when(votingService.hasUserVoted(testUserId, testElectionId)).thenReturn(false);
        
        // Act & Assert
        mockMvc.perform(get("/voting/elections").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("voting/elections"))
                .andExpect(model().attribute("elections", activeElections))
                .andExpect(model().attribute("userId", testUserId))
                .andExpect(model().attribute("hasVoted_" + testElectionId, false));
        
        verify(votingService).getActiveElections();
        verify(votingService).hasUserVoted(testUserId, testElectionId);
    }
    
    @Test
    void testShowElections_NotAuthenticated() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/voting/elections"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
    
    @Test
    void testShowElections_ServiceException() throws Exception {
        // Arrange
        when(votingService.getActiveElections()).thenThrow(new RuntimeException("Service error"));
        
        // Act & Assert
        mockMvc.perform(get("/voting/elections").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("voting/elections"))
                .andExpect(model().attribute("error", "Failed to load elections. Please try again."));
        
        verify(votingService).getActiveElections();
    }
    
    @Test
    void testShowElectionDetails_Success() throws Exception {
        // Arrange
        when(votingService.isElectionActive(testElectionId)).thenReturn(true);
        when(votingService.hasUserVoted(testUserId, testElectionId)).thenReturn(false);
        when(votingService.getElectionDetails(testElectionId)).thenReturn(testElection);
        
        // Act & Assert
        mockMvc.perform(get("/voting/election/" + testElectionId).session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("voting/election-details"))
                .andExpect(model().attribute("election", testElection))
                .andExpect(model().attribute("candidates", testElection.getCandidates()))
                .andExpect(model().attribute("userId", testUserId));
        
        verify(votingService).isElectionActive(testElectionId);
        verify(votingService).hasUserVoted(testUserId, testElectionId);
        verify(votingService).getElectionDetails(testElectionId);
    }
    
    @Test
    void testShowElectionDetails_AlreadyVoted() throws Exception {
        // Arrange
        when(votingService.isElectionActive(testElectionId)).thenReturn(true);
        when(votingService.hasUserVoted(testUserId, testElectionId)).thenReturn(true);
        when(votingService.getElectionDetails(testElectionId)).thenReturn(testElection);
        
        // Act & Assert
        mockMvc.perform(get("/voting/election/" + testElectionId).session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("voting/election-details"))
                .andExpect(model().attribute("message", "You have already voted in this election."))
                .andExpect(model().attribute("alreadyVoted", true));
        
        verify(votingService).hasUserVoted(testUserId, testElectionId);
    }
    
    @Test
    void testShowElectionDetails_ElectionNotActive() throws Exception {
        // Arrange
        when(votingService.isElectionActive(testElectionId)).thenReturn(false);
        
        // Act & Assert
        mockMvc.perform(get("/voting/election/" + testElectionId).session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("voting/elections"))
                .andExpect(model().attribute("error", "Election is not available for voting."));
        
        verify(votingService).isElectionActive(testElectionId);
    }
    
    @Test
    void testShowElectionDetails_ElectionNotFound() throws Exception {
        // Arrange
        when(votingService.isElectionActive(testElectionId)).thenReturn(true);
        when(votingService.hasUserVoted(testUserId, testElectionId)).thenReturn(false);
        when(votingService.getElectionDetails(testElectionId)).thenReturn(null);
        
        // Act & Assert
        mockMvc.perform(get("/voting/election/" + testElectionId).session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("voting/elections"))
                .andExpect(model().attribute("error", "Election not found."));
        
        verify(votingService).getElectionDetails(testElectionId);
    }
    
    @Test
    void testShowElectionDetails_InvalidElectionId() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/voting/election/invalid-id!@#").session(session))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void testCastVote_Success() throws Exception {
        // Arrange
        String voteId = UUID.randomUUID().toString();
        VoteResult successResult = VoteResult.success(voteId, "Vote cast successfully");
        
        when(votingService.isElectionActive(testElectionId)).thenReturn(true);
        when(votingService.isCandidateValid(testCandidateId, testElectionId)).thenReturn(true);
        when(votingService.hasUserVoted(testUserId, testElectionId)).thenReturn(false);
        when(votingService.castVote(eq(testUserId), eq(testCandidateId), eq(testElectionId), anyString(), anyString()))
                .thenReturn(successResult);
        
        // Act & Assert
        mockMvc.perform(post("/voting/cast-vote")
                .with(csrf())
                .session(session)
                .param("candidateId", testCandidateId)
                .param("electionId", testElectionId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/voting/confirmation"))
                .andExpect(flash().attribute("message", "Your vote has been cast successfully!"))
                .andExpect(flash().attribute("voteId", voteId));
        
        verify(votingService).castVote(eq(testUserId), eq(testCandidateId), eq(testElectionId), anyString(), anyString());
    }
    
    @Test
    void testCastVote_ElectionNotActive() throws Exception {
        // Arrange
        when(votingService.isElectionActive(testElectionId)).thenReturn(false);
        
        // Act & Assert
        mockMvc.perform(post("/voting/cast-vote")
                .with(csrf())
                .session(session)
                .param("candidateId", testCandidateId)
                .param("electionId", testElectionId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/voting/elections"))
                .andExpect(flash().attribute("error", "Election is not available for voting."));
        
        verify(votingService).isElectionActive(testElectionId);
    }
    
    @Test
    void testCastVote_InvalidCandidate() throws Exception {
        // Arrange
        when(votingService.isElectionActive(testElectionId)).thenReturn(true);
        when(votingService.isCandidateValid(testCandidateId, testElectionId)).thenReturn(false);
        
        // Act & Assert
        mockMvc.perform(post("/voting/cast-vote")
                .with(csrf())
                .session(session)
                .param("candidateId", testCandidateId)
                .param("electionId", testElectionId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/voting/election/" + testElectionId))
                .andExpect(flash().attribute("error", "Invalid candidate selection."));
        
        verify(votingService).isCandidateValid(testCandidateId, testElectionId);
    }
    
    @Test
    void testCastVote_AlreadyVoted() throws Exception {
        // Arrange
        when(votingService.isElectionActive(testElectionId)).thenReturn(true);
        when(votingService.isCandidateValid(testCandidateId, testElectionId)).thenReturn(true);
        when(votingService.hasUserVoted(testUserId, testElectionId)).thenReturn(true);
        
        // Act & Assert
        mockMvc.perform(post("/voting/cast-vote")
                .with(csrf())
                .session(session)
                .param("candidateId", testCandidateId)
                .param("electionId", testElectionId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/voting/election/" + testElectionId))
                .andExpect(flash().attribute("error", "You have already voted in this election."));
        
        verify(votingService).hasUserVoted(testUserId, testElectionId);
    }
    
    @Test
    void testCastVote_VotingFailure() throws Exception {
        // Arrange
        VoteResult failureResult = VoteResult.failure("Voting service error");
        
        when(votingService.isElectionActive(testElectionId)).thenReturn(true);
        when(votingService.isCandidateValid(testCandidateId, testElectionId)).thenReturn(true);
        when(votingService.hasUserVoted(testUserId, testElectionId)).thenReturn(false);
        when(votingService.castVote(eq(testUserId), eq(testCandidateId), eq(testElectionId), anyString(), anyString()))
                .thenReturn(failureResult);
        
        // Act & Assert
        mockMvc.perform(post("/voting/cast-vote")
                .with(csrf())
                .session(session)
                .param("candidateId", testCandidateId)
                .param("electionId", testElectionId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/voting/election/" + testElectionId))
                .andExpect(flash().attribute("error", "Voting service error"));
        
        verify(votingService).castVote(eq(testUserId), eq(testCandidateId), eq(testElectionId), anyString(), anyString());
    }
    
    @Test
    void testCastVote_InvalidCandidateId() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/voting/cast-vote")
                .with(csrf())
                .session(session)
                .param("candidateId", "invalid-id!@#")
                .param("electionId", testElectionId))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void testCastVote_BlankCandidateId() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/voting/cast-vote")
                .with(csrf())
                .session(session)
                .param("candidateId", "")
                .param("electionId", testElectionId))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void testCastVote_NotAuthenticated() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/voting/cast-vote")
                .with(csrf())
                .param("candidateId", testCandidateId)
                .param("electionId", testElectionId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
    
    @Test
    void testShowVoteConfirmation_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/voting/confirmation").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("voting/confirmation"))
                .andExpect(model().attribute("userId", testUserId));
    }
    
    @Test
    void testShowVoteConfirmation_NotAuthenticated() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/voting/confirmation"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
    
    @Test
    void testShowVotingHistory_Success() throws Exception {
        // Arrange
        List<Election> allElections = Arrays.asList(testElection);
        Vote testVote = new Vote();
        testVote.setVoteId(UUID.randomUUID().toString());
        testVote.setTimestamp(LocalDateTime.now());
        
        when(votingService.getActiveElections()).thenReturn(allElections);
        when(votingService.hasUserVoted(testUserId, testElectionId)).thenReturn(true);
        when(votingService.getUserVote(testUserId, testElectionId)).thenReturn(testVote);
        
        // Act & Assert
        mockMvc.perform(get("/voting/history").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("voting/history"))
                .andExpect(model().attribute("elections", allElections))
                .andExpect(model().attribute("userId", testUserId))
                .andExpect(model().attribute("hasVoted_" + testElectionId, true))
                .andExpect(model().attribute("voteTimestamp_" + testElectionId, testVote.getTimestamp()))
                .andExpect(model().attribute("voteId_" + testElectionId, testVote.getVoteId()));
        
        verify(votingService).getActiveElections();
        verify(votingService).hasUserVoted(testUserId, testElectionId);
        verify(votingService).getUserVote(testUserId, testElectionId);
    }
    
    @Test
    void testShowVotingHistory_NotAuthenticated() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/voting/history"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
    
    @Test
    void testShowVotingHistory_ServiceException() throws Exception {
        // Arrange
        when(votingService.getActiveElections()).thenThrow(new RuntimeException("Service error"));
        
        // Act & Assert
        mockMvc.perform(get("/voting/history").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("voting/history"))
                .andExpect(model().attribute("error", "Failed to load voting history. Please try again."));
        
        verify(votingService).getActiveElections();
    }
}