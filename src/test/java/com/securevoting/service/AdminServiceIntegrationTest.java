package com.securevoting.service;

import com.securevoting.model.*;
import com.securevoting.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for AdminService.
 * Tests the complete workflow with real database interactions.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminServiceIntegrationTest {
    
    @Autowired
    private AdminService adminService;
    
    @Autowired
    private ElectionRepository electionRepository;
    
    @Autowired
    private CandidateRepository candidateRepository;
    
    @Autowired
    private VoteRepository voteRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private AuditLogRepository auditLogRepository;
    
    @Autowired
    private CryptographyService cryptographyService;
    
    @Autowired
    private VotingService votingService;
    
    private User testAdmin;
    private User testVoter;
    private ElectionRequest validElectionRequest;
    
    @BeforeEach
    void setUp() {
        // Clean up any existing data
        voteRepository.deleteAll();
        candidateRepository.deleteAll();
        electionRepository.deleteAll();
        auditLogRepository.deleteAll();
        userRepository.deleteAll();
        
        // Create test users
        testAdmin = new User();
        testAdmin.setUserId(UUID.randomUUID().toString());
        testAdmin.setUsername("testadmin");
        testAdmin.setEmail("admin@test.com");
        testAdmin.setPasswordHash("hashedpassword");
        testAdmin.setRole(UserRole.ADMIN);
        testAdmin.setEnabled(true);
        testAdmin = userRepository.save(testAdmin);
        
        testVoter = new User();
        testVoter.setUserId(UUID.randomUUID().toString());
        testVoter.setUsername("testvoter");
        testVoter.setEmail("voter@test.com");
        testVoter.setPasswordHash("hashedpassword");
        testVoter.setRole(UserRole.VOTER);
        testVoter.setEnabled(true);
        testVoter = userRepository.save(testVoter);
        
        // Create test election request
        ElectionRequest.CandidateRequest candidate1 = new ElectionRequest.CandidateRequest(
            "Alice Johnson", "Experienced leader with vision", 1);
        ElectionRequest.CandidateRequest candidate2 = new ElectionRequest.CandidateRequest(
            "Bob Wilson", "Fresh perspective and innovation", 2);
        ElectionRequest.CandidateRequest candidate3 = new ElectionRequest.CandidateRequest(
            "Carol Davis", "Community-focused approach", 3);
        
        validElectionRequest = new ElectionRequest(
            "Integration Test Election",
            "A comprehensive test election for integration testing",
            LocalDateTime.now().plusDays(1),
            LocalDateTime.now().plusDays(7),
            Arrays.asList(candidate1, candidate2, candidate3)
        );
    }
    
    @Test
    void completeElectionWorkflow_CreateActivateVoteTallyComplete_ShouldWorkEndToEnd() {
        // 1. Create election
        Election election = adminService.createElection(validElectionRequest, testAdmin.getUserId());
        
        assertNotNull(election);
        assertEquals("Integration Test Election", election.getTitle());
        assertEquals(ElectionStatus.DRAFT, election.getStatus());
        
        // Verify candidates were created
        List<Candidate> candidates = candidateRepository.findByElectionIdOrderByDisplayOrderAsc(election.getElectionId());
        assertEquals(3, candidates.size());
        assertEquals("Alice Johnson", candidates.get(0).getName());
        assertEquals("Bob Wilson", candidates.get(1).getName());
        assertEquals("Carol Davis", candidates.get(2).getName());
        
        // 2. Activate election
        Election activatedElection = adminService.activateElection(election.getElectionId(), testAdmin.getUserId());
        assertEquals(ElectionStatus.ACTIVE, activatedElection.getStatus());
        
        // 3. Simulate voting (modify election time to allow voting)
        election.setStartTime(LocalDateTime.now().minusHours(1));
        election.setEndTime(LocalDateTime.now().plusHours(1));
        electionRepository.save(election);
        
        // Cast some votes
        VoteResult vote1 = votingService.castVote(testVoter.getUserId(), 
            candidates.get(0).getCandidateId(), election.getElectionId(), "127.0.0.1", "TestAgent");
        assertTrue(vote1.isSuccessful());
        
        // Create additional test voters and votes
        User voter2 = createTestVoter("voter2@test.com", "testvoter2");
        User voter3 = createTestVoter("voter3@test.com", "testvoter3");
        
        VoteResult vote2 = votingService.castVote(voter2.getUserId(), 
            candidates.get(1).getCandidateId(), election.getElectionId(), "127.0.0.1", "TestAgent");
        assertTrue(vote2.isSuccessful());
        
        VoteResult vote3 = votingService.castVote(voter3.getUserId(), 
            candidates.get(0).getCandidateId(), election.getElectionId(), "127.0.0.1", "TestAgent");
        assertTrue(vote3.isSuccessful());
        
        // 4. Tally votes
        ElectionResult results = adminService.tallyVotes(election.getElectionId());
        
        assertNotNull(results);
        assertEquals(election.getElectionId(), results.getElectionId());
        assertEquals(3, results.getTotalVotes());
        assertEquals(3, results.getValidVotes());
        assertEquals(0, results.getInvalidVotes());
        assertTrue(results.isTallySuccessful());
        
        // Verify vote counts
        ElectionResult.CandidateResult aliceResult = results.getCandidateResults().get(candidates.get(0).getCandidateId());
        ElectionResult.CandidateResult bobResult = results.getCandidateResults().get(candidates.get(1).getCandidateId());
        ElectionResult.CandidateResult carolResult = results.getCandidateResults().get(candidates.get(2).getCandidateId());
        
        assertEquals(2, aliceResult.getVoteCount()); // Alice got 2 votes
        assertEquals(1, bobResult.getVoteCount());   // Bob got 1 vote
        assertEquals(0, carolResult.getVoteCount()); // Carol got 0 votes
        
        assertEquals(66.67, aliceResult.getPercentage(), 0.1);
        assertEquals(33.33, bobResult.getPercentage(), 0.1);
        assertEquals(0.0, carolResult.getPercentage(), 0.1);
        
        // 5. Complete election
        Election completedElection = adminService.completeElection(election.getElectionId(), testAdmin.getUserId());
        assertEquals(ElectionStatus.COMPLETED, completedElection.getStatus());
        
        // 6. Verify audit logs were created
        List<AuditLog> auditLogs = auditLogRepository.findByUserIdOrderByTimestampDesc(testAdmin.getUserId());
        assertTrue(auditLogs.size() >= 4); // CREATE, ACTIVATE, TALLY, COMPLETE
        
        // Verify specific audit actions
        boolean hasCreateAction = auditLogs.stream().anyMatch(log -> log.getAction().equals("CREATE_ELECTION"));
        boolean hasActivateAction = auditLogs.stream().anyMatch(log -> log.getAction().equals("ACTIVATE_ELECTION"));
        boolean hasCompleteAction = auditLogs.stream().anyMatch(log -> log.getAction().equals("COMPLETE_ELECTION"));
        
        assertTrue(hasCreateAction);
        assertTrue(hasActivateAction);
        assertTrue(hasCompleteAction);
    }
    
    @Test
    void updateElection_DraftElection_ShouldUpdateSuccessfully() {
        // Create initial election
        Election election = adminService.createElection(validElectionRequest, testAdmin.getUserId());
        
        // Modify the request
        ElectionRequest updateRequest = new ElectionRequest(
            "Updated Election Title",
            "Updated description",
            LocalDateTime.now().plusDays(2),
            LocalDateTime.now().plusDays(8),
            Arrays.asList(
                new ElectionRequest.CandidateRequest("Updated Candidate 1", "Updated desc 1", 1),
                new ElectionRequest.CandidateRequest("Updated Candidate 2", "Updated desc 2", 2)
            )
        );
        
        // Update election
        Election updatedElection = adminService.updateElection(election.getElectionId(), updateRequest, testAdmin.getUserId());
        
        assertEquals("Updated Election Title", updatedElection.getTitle());
        assertEquals("Updated description", updatedElection.getDescription());
        
        // Verify candidates were updated
        List<Candidate> updatedCandidates = candidateRepository.findByElectionIdOrderByDisplayOrderAsc(election.getElectionId());
        assertEquals(2, updatedCandidates.size());
        assertEquals("Updated Candidate 1", updatedCandidates.get(0).getName());
        assertEquals("Updated Candidate 2", updatedCandidates.get(1).getName());
    }
    
    @Test
    void deleteElection_DraftElectionWithoutVotes_ShouldDeleteSuccessfully() {
        // Create election
        Election election = adminService.createElection(validElectionRequest, testAdmin.getUserId());
        String electionId = election.getElectionId();
        
        // Verify election and candidates exist
        assertTrue(electionRepository.existsById(electionId));
        assertEquals(3, candidateRepository.countByElectionId(electionId));
        
        // Delete election
        adminService.deleteElection(electionId, testAdmin.getUserId());
        
        // Verify election and candidates are deleted
        assertFalse(electionRepository.existsById(electionId));
        assertEquals(0, candidateRepository.countByElectionId(electionId));
    }
    
    @Test
    void getSystemStatistics_WithRealData_ShouldReturnAccurateStats() {
        // Create some test data
        Election election1 = adminService.createElection(validElectionRequest, testAdmin.getUserId());
        adminService.activateElection(election1.getElectionId(), testAdmin.getUserId());
        
        ElectionRequest request2 = new ElectionRequest(
            "Second Election",
            "Another test election",
            LocalDateTime.now().plusDays(10),
            LocalDateTime.now().plusDays(17),
            Arrays.asList(
                new ElectionRequest.CandidateRequest("Candidate A", "Description A", 1),
                new ElectionRequest.CandidateRequest("Candidate B", "Description B", 2)
            )
        );
        Election election2 = adminService.createElection(request2, testAdmin.getUserId());
        
        // Get statistics
        SystemStatistics stats = adminService.getSystemStatistics();
        
        assertNotNull(stats);
        assertEquals(2, stats.getTotalUsers()); // testAdmin + testVoter
        assertEquals(2, stats.getTotalElections());
        assertEquals(1, stats.getActiveElections());
        assertEquals(0, stats.getCompletedElections());
        
        assertNotNull(stats.getElectionStatusCounts());
        assertEquals(1, stats.getElectionStatusCounts().get("DRAFT").intValue());
        assertEquals(1, stats.getElectionStatusCounts().get("ACTIVE").intValue());
        assertEquals(0, stats.getElectionStatusCounts().get("COMPLETED").intValue());
    }
    
    @Test
    void validateElectionIntegrity_WithValidVotes_ShouldReturnNoIssues() {
        // Create and activate election
        Election election = adminService.createElection(validElectionRequest, testAdmin.getUserId());
        adminService.activateElection(election.getElectionId(), testAdmin.getUserId());
        
        // Modify election time to allow voting
        election.setStartTime(LocalDateTime.now().minusHours(1));
        election.setEndTime(LocalDateTime.now().plusHours(1));
        electionRepository.save(election);
        
        // Cast a vote
        List<Candidate> candidates = candidateRepository.findByElectionIdOrderByDisplayOrderAsc(election.getElectionId());
        VoteResult voteResult = votingService.castVote(testVoter.getUserId(), 
            candidates.get(0).getCandidateId(), election.getElectionId(), "127.0.0.1", "TestAgent");
        assertTrue(voteResult.isSuccessful());
        
        // Validate integrity
        List<String> issues = adminService.validateElectionIntegrity(election.getElectionId());
        
        assertTrue(issues.isEmpty(), "Should have no integrity issues with valid votes");
    }
    
    @Test
    void getAuditLogs_WithFiltering_ShouldReturnFilteredResults() {
        // Create some audit activity
        Election election = adminService.createElection(validElectionRequest, testAdmin.getUserId());
        adminService.activateElection(election.getElectionId(), testAdmin.getUserId());
        
        // Create filter for admin actions
        AuditFilter filter = AuditFilter.forUser(testAdmin.getUserId());
        
        List<AuditLog> auditLogs = adminService.getAuditLogs(filter);
        
        assertNotNull(auditLogs);
        assertFalse(auditLogs.isEmpty());
        
        // All logs should be for the test admin
        assertTrue(auditLogs.stream().allMatch(log -> testAdmin.getUserId().equals(log.getUserId())));
    }
    
    private User createTestVoter(String email, String username) {
        User voter = new User();
        voter.setUserId(UUID.randomUUID().toString());
        voter.setUsername(username);
        voter.setEmail(email);
        voter.setPasswordHash("hashedpassword");
        voter.setRole(UserRole.VOTER);
        voter.setEnabled(true);
        return userRepository.save(voter);
    }
}