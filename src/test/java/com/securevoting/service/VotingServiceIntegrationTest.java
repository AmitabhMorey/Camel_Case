package com.securevoting.service;

import com.securevoting.model.*;
import com.securevoting.repository.CandidateRepository;
import com.securevoting.repository.ElectionRepository;
import com.securevoting.repository.UserRepository;
import com.securevoting.repository.VoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for VotingService with real database interactions.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VotingServiceIntegrationTest {
    
    @Autowired
    private VotingService votingService;
    
    @Autowired
    private VoteRepository voteRepository;
    
    @Autowired
    private ElectionRepository electionRepository;
    
    @Autowired
    private CandidateRepository candidateRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    private User testUser;
    private Election testElection;
    private Candidate testCandidate1;
    private Candidate testCandidate2;
    
    @BeforeEach
    void setUp() {
        // Clean up any existing test data
        voteRepository.deleteAll();
        candidateRepository.deleteAll();
        electionRepository.deleteAll();
        userRepository.deleteAll();
        
        // Create test user
        testUser = new User();
        testUser.setUserId(UUID.randomUUID().toString());
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hashedpassword");
        testUser.setEnabled(true);
        testUser.setRole(UserRole.VOTER);
        testUser = userRepository.save(testUser);
        
        // Create test election
        testElection = new Election();
        testElection.setElectionId(UUID.randomUUID().toString());
        testElection.setTitle("Integration Test Election");
        testElection.setDescription("Test election for integration testing");
        testElection.setStartTime(LocalDateTime.now().minusHours(1));
        testElection.setEndTime(LocalDateTime.now().plusHours(1));
        testElection.setStatus(ElectionStatus.ACTIVE);
        testElection.setCreatedBy("admin");
        testElection = electionRepository.save(testElection);
        
        // Create test candidates
        testCandidate1 = new Candidate();
        testCandidate1.setCandidateId(UUID.randomUUID().toString());
        testCandidate1.setName("Candidate One");
        testCandidate1.setDescription("First test candidate");
        testCandidate1.setElectionId(testElection.getElectionId());
        testCandidate1.setDisplayOrder(1);
        testCandidate1.setEnabled(true);
        testCandidate1 = candidateRepository.save(testCandidate1);
        
        testCandidate2 = new Candidate();
        testCandidate2.setCandidateId(UUID.randomUUID().toString());
        testCandidate2.setName("Candidate Two");
        testCandidate2.setDescription("Second test candidate");
        testCandidate2.setElectionId(testElection.getElectionId());
        testCandidate2.setDisplayOrder(2);
        testCandidate2.setEnabled(true);
        testCandidate2 = candidateRepository.save(testCandidate2);
    }
    
    @Test
    void castVote_FullWorkflow_Success() {
        // Arrange
        String ipAddress = "192.168.1.100";
        String userAgent = "Integration Test Agent";
        
        // Verify initial state
        assertFalse(votingService.hasUserVoted(testUser.getUserId(), testElection.getElectionId()));
        assertEquals(0, votingService.getVoteCount(testElection.getElectionId()));
        
        // Act - Cast vote
        VoteResult result = votingService.castVote(
            testUser.getUserId(),
            testCandidate1.getCandidateId(),
            testElection.getElectionId(),
            ipAddress,
            userAgent
        );
        
        // Assert - Vote casting success
        assertTrue(result.isSuccessful());
        assertNotNull(result.getVoteId());
        assertEquals("Your vote has been cast successfully", result.getMessage());
        
        // Verify vote was stored
        assertTrue(votingService.hasUserVoted(testUser.getUserId(), testElection.getElectionId()));
        assertEquals(1, votingService.getVoteCount(testElection.getElectionId()));
        
        // Verify vote details
        Vote storedVote = votingService.getUserVote(testUser.getUserId(), testElection.getElectionId());
        assertNotNull(storedVote);
        assertEquals(testUser.getUserId(), storedVote.getUserId());
        assertEquals(testElection.getElectionId(), storedVote.getElectionId());
        assertEquals(ipAddress, storedVote.getIpAddress());
        assertEquals(userAgent, storedVote.getUserAgent());
        assertNotNull(storedVote.getEncryptedVoteData());
        assertNotNull(storedVote.getVoteHash());
        assertNotNull(storedVote.getInitializationVector());
    }
    
    @Test
    void castVote_DuplicateVotePrevention() {
        // Arrange - Cast first vote
        VoteResult firstVote = votingService.castVote(
            testUser.getUserId(),
            testCandidate1.getCandidateId(),
            testElection.getElectionId(),
            "192.168.1.100",
            "Test Agent"
        );
        assertTrue(firstVote.isSuccessful());
        
        // Act - Attempt duplicate vote
        VoteResult duplicateVote = votingService.castVote(
            testUser.getUserId(),
            testCandidate2.getCandidateId(),
            testElection.getElectionId(),
            "192.168.1.100",
            "Test Agent"
        );
        
        // Assert - Duplicate vote prevented
        assertFalse(duplicateVote.isSuccessful());
        assertEquals("You have already voted in this election", duplicateVote.getMessage());
        assertEquals("DUPLICATE_VOTE", duplicateVote.getErrorCode());
        
        // Verify only one vote exists
        assertEquals(1, votingService.getVoteCount(testElection.getElectionId()));
    }
    
    @Test
    void getActiveElections_ReturnsOnlyActiveElections() {
        // Arrange - Create additional elections with different statuses
        Election draftElection = new Election();
        draftElection.setElectionId(UUID.randomUUID().toString());
        draftElection.setTitle("Draft Election");
        draftElection.setStatus(ElectionStatus.DRAFT);
        draftElection.setStartTime(LocalDateTime.now().plusHours(1));
        draftElection.setEndTime(LocalDateTime.now().plusHours(2));
        draftElection.setCreatedBy("admin");
        electionRepository.save(draftElection);
        
        Election completedElection = new Election();
        completedElection.setElectionId(UUID.randomUUID().toString());
        completedElection.setTitle("Completed Election");
        completedElection.setStatus(ElectionStatus.COMPLETED);
        completedElection.setStartTime(LocalDateTime.now().minusHours(3));
        completedElection.setEndTime(LocalDateTime.now().minusHours(1));
        completedElection.setCreatedBy("admin");
        electionRepository.save(completedElection);
        
        // Act
        List<Election> activeElections = votingService.getActiveElections();
        
        // Assert - Only the active election is returned
        assertEquals(1, activeElections.size());
        assertEquals(testElection.getElectionId(), activeElections.get(0).getElectionId());
    }
    
    @Test
    void isElectionActive_VariousElectionStates() {
        // Test active election
        assertTrue(votingService.isElectionActive(testElection.getElectionId()));
        
        // Create and test draft election
        Election draftElection = new Election();
        draftElection.setElectionId(UUID.randomUUID().toString());
        draftElection.setTitle("Draft Election");
        draftElection.setStatus(ElectionStatus.DRAFT);
        draftElection.setStartTime(LocalDateTime.now().plusHours(1));
        draftElection.setEndTime(LocalDateTime.now().plusHours(2));
        draftElection.setCreatedBy("admin");
        draftElection = electionRepository.save(draftElection);
        
        assertFalse(votingService.isElectionActive(draftElection.getElectionId()));
        
        // Create and test expired election
        Election expiredElection = new Election();
        expiredElection.setElectionId(UUID.randomUUID().toString());
        expiredElection.setTitle("Expired Election");
        expiredElection.setStatus(ElectionStatus.ACTIVE);
        expiredElection.setStartTime(LocalDateTime.now().minusHours(3));
        expiredElection.setEndTime(LocalDateTime.now().minusHours(1));
        expiredElection.setCreatedBy("admin");
        expiredElection = electionRepository.save(expiredElection);
        
        assertFalse(votingService.isElectionActive(expiredElection.getElectionId()));
    }
    
    @Test
    void isCandidateValid_VariousCandidateStates() {
        // Test valid enabled candidate
        assertTrue(votingService.isCandidateValid(testCandidate1.getCandidateId(), testElection.getElectionId()));
        
        // Test candidate from different election
        Election otherElection = new Election();
        otherElection.setElectionId(UUID.randomUUID().toString());
        otherElection.setTitle("Other Election");
        otherElection.setStatus(ElectionStatus.ACTIVE);
        otherElection.setStartTime(LocalDateTime.now().minusHours(1));
        otherElection.setEndTime(LocalDateTime.now().plusHours(1));
        otherElection.setCreatedBy("admin");
        otherElection = electionRepository.save(otherElection);
        
        assertFalse(votingService.isCandidateValid(testCandidate1.getCandidateId(), otherElection.getElectionId()));
        
        // Test disabled candidate
        testCandidate2.setEnabled(false);
        candidateRepository.save(testCandidate2);
        
        assertFalse(votingService.isCandidateValid(testCandidate2.getCandidateId(), testElection.getElectionId()));
    }
    
    @Test
    void getElectionDetails_WithCandidates() {
        // Act
        Election electionDetails = votingService.getElectionDetails(testElection.getElectionId());
        
        // Assert
        assertNotNull(electionDetails);
        assertEquals(testElection.getElectionId(), electionDetails.getElectionId());
        assertEquals(testElection.getTitle(), electionDetails.getTitle());
        assertEquals(testElection.getStatus(), electionDetails.getStatus());
    }
    
    @Test
    void multipleUsersVoting_ConcurrentScenario() {
        // Arrange - Create additional users
        User user2 = new User();
        user2.setUserId(UUID.randomUUID().toString());
        user2.setUsername("testuser2");
        user2.setEmail("test2@example.com");
        user2.setPasswordHash("hashedpassword2");
        user2.setEnabled(true);
        user2.setRole(UserRole.VOTER);
        user2 = userRepository.save(user2);
        
        User user3 = new User();
        user3.setUserId(UUID.randomUUID().toString());
        user3.setUsername("testuser3");
        user3.setEmail("test3@example.com");
        user3.setPasswordHash("hashedpassword3");
        user3.setEnabled(true);
        user3.setRole(UserRole.VOTER);
        user3 = userRepository.save(user3);
        
        // Act - Multiple users vote for different candidates
        VoteResult vote1 = votingService.castVote(
            testUser.getUserId(),
            testCandidate1.getCandidateId(),
            testElection.getElectionId(),
            "192.168.1.100",
            "Agent1"
        );
        
        VoteResult vote2 = votingService.castVote(
            user2.getUserId(),
            testCandidate2.getCandidateId(),
            testElection.getElectionId(),
            "192.168.1.101",
            "Agent2"
        );
        
        VoteResult vote3 = votingService.castVote(
            user3.getUserId(),
            testCandidate1.getCandidateId(),
            testElection.getElectionId(),
            "192.168.1.102",
            "Agent3"
        );
        
        // Assert - All votes successful
        assertTrue(vote1.isSuccessful());
        assertTrue(vote2.isSuccessful());
        assertTrue(vote3.isSuccessful());
        
        // Verify vote count
        assertEquals(3, votingService.getVoteCount(testElection.getElectionId()));
        
        // Verify each user has voted
        assertTrue(votingService.hasUserVoted(testUser.getUserId(), testElection.getElectionId()));
        assertTrue(votingService.hasUserVoted(user2.getUserId(), testElection.getElectionId()));
        assertTrue(votingService.hasUserVoted(user3.getUserId(), testElection.getElectionId()));
    }
}