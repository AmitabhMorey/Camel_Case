package com.securevoting.integration;

import com.securevoting.model.*;
import com.securevoting.repository.*;
import com.securevoting.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for complete user workflows from registration to voting
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class UserWorkflowIntegrationTest {

    @Autowired
    private AuthenticationService authenticationService;
    
    @Autowired
    private VotingService votingService;
    
    @Autowired
    private AdminService adminService;
    
    @Autowired
    private QRCodeService qrCodeService;
    
    @Autowired
    private OTPService otpService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ElectionRepository electionRepository;
    
    @Autowired
    private CandidateRepository candidateRepository;
    
    @Autowired
    private VoteRepository voteRepository;

    private User testUser;
    private Election testElection;
    private Candidate testCandidate1;
    private Candidate testCandidate2;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setUserId("test-user-1");
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("$2a$10$hashedpassword");
        testUser.setQrCodeSecret("test-qr-secret");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setEnabled(true);
        testUser.setRole(UserRole.VOTER);
        userRepository.save(testUser);

        // Create test election
        testElection = new Election();
        testElection.setElectionId("test-election-1");
        testElection.setTitle("Test Election");
        testElection.setDescription("Integration test election");
        testElection.setStartTime(LocalDateTime.now().minusHours(1));
        testElection.setEndTime(LocalDateTime.now().plusHours(1));
        testElection.setStatus(ElectionStatus.ACTIVE);
        electionRepository.save(testElection);

        // Create test candidates
        testCandidate1 = new Candidate();
        testCandidate1.setCandidateId("candidate-1");
        testCandidate1.setName("John Doe");
        testCandidate1.setDescription("First candidate");
        testCandidate1.setElectionId(testElection.getElectionId());
        candidateRepository.save(testCandidate1);

        testCandidate2 = new Candidate();
        testCandidate2.setCandidateId("candidate-2");
        testCandidate2.setName("Jane Smith");
        testCandidate2.setDescription("Second candidate");
        testCandidate2.setElectionId(testElection.getElectionId());
        candidateRepository.save(testCandidate2);
    }

    @Test
    void testCompleteVotingWorkflow() {
        // Step 1: Generate QR code for user
        String qrData = qrCodeService.generateQRCodeData(testUser.getUserId());
        assertNotNull(qrData);
        assertTrue(qrData.contains(testUser.getUserId()));

        // Step 2: Validate QR code
        boolean qrValid = qrCodeService.validateQRCodeData(qrData, testUser.getUserId());
        assertTrue(qrValid);

        // Step 3: Authenticate with QR code
        AuthenticationResult qrResult = authenticationService.authenticateWithQR(
            testUser.getUserId(), qrData);
        assertTrue(qrResult.isSuccessful());

        // Step 4: Generate OTP
        String otp = otpService.generateOTP(testUser.getUserId());
        assertNotNull(otp);
        assertEquals(6, otp.length());

        // Step 5: Authenticate with OTP
        AuthenticationResult otpResult = authenticationService.authenticateWithOTP(
            testUser.getUserId(), otp);
        assertTrue(otpResult.isSuccessful());
        assertNotNull(otpResult.getSessionId());

        // Step 6: Get active elections
        List<Election> activeElections = votingService.getActiveElections();
        assertFalse(activeElections.isEmpty());
        assertTrue(activeElections.stream()
            .anyMatch(e -> e.getElectionId().equals(testElection.getElectionId())));

        // Step 7: Check if user has already voted
        boolean hasVoted = votingService.hasUserVoted(testUser.getUserId(), testElection.getElectionId());
        assertFalse(hasVoted);

        // Step 8: Cast vote
        VoteResult voteResult = votingService.castVote(
            testUser.getUserId(), 
            testCandidate1.getCandidateId(), 
            testElection.getElectionId(),
            "127.0.0.1",
            "Test-User-Agent"
        );
        assertTrue(voteResult.isSuccessful());
        assertNotNull(voteResult.getVoteId());

        // Step 9: Verify vote was recorded
        boolean hasVotedAfter = votingService.hasUserVoted(testUser.getUserId(), testElection.getElectionId());
        assertTrue(hasVotedAfter);

        // Step 10: Attempt duplicate vote (should fail)
        VoteResult duplicateVoteResult = votingService.castVote(
            testUser.getUserId(), 
            testCandidate2.getCandidateId(), 
            testElection.getElectionId(),
            "127.0.0.1",
            "Test-User-Agent"
        );
        assertFalse(duplicateVoteResult.isSuccessful());
        assertTrue(duplicateVoteResult.getMessage().contains("already voted"));
    }

    @Test
    void testAuthenticationFailureWorkflow() {
        // Test invalid QR code
        AuthenticationResult invalidQrResult = authenticationService.authenticateWithQR(
            testUser.getUserId(), "invalid-qr-data");
        assertFalse(invalidQrResult.isSuccessful());
        assertNotNull(invalidQrResult.getErrorMessage());

        // Test invalid OTP
        AuthenticationResult invalidOtpResult = authenticationService.authenticateWithOTP(
            testUser.getUserId(), "123456");
        assertFalse(invalidOtpResult.isSuccessful());
        assertNotNull(invalidOtpResult.getErrorMessage());
    }

    @Test
    void testVotingInInactiveElection() {
        // Create inactive election
        Election inactiveElection = new Election();
        inactiveElection.setElectionId("inactive-election");
        inactiveElection.setTitle("Inactive Election");
        inactiveElection.setDescription("Test inactive election");
        inactiveElection.setStartTime(LocalDateTime.now().minusHours(2));
        inactiveElection.setEndTime(LocalDateTime.now().minusHours(1));
        inactiveElection.setStatus(ElectionStatus.COMPLETED);
        electionRepository.save(inactiveElection);

        // Attempt to vote in inactive election
        VoteResult voteResult = votingService.castVote(
            testUser.getUserId(), 
            testCandidate1.getCandidateId(), 
            inactiveElection.getElectionId(),
            "127.0.0.1",
            "Test-User-Agent"
        );
        assertFalse(voteResult.isSuccessful());
        assertTrue(voteResult.getMessage().contains("not active"));
    }

    @Test
    void testAdminElectionManagementWorkflow() {
        // Create admin user
        User adminUser = new User();
        adminUser.setUserId("admin-user");
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setPasswordHash("$2a$10$hashedpassword");
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setEnabled(true);
        adminUser.setCreatedAt(LocalDateTime.now());
        userRepository.save(adminUser);

        // Create election request
        ElectionRequest request = new ElectionRequest();
        request.setTitle("Admin Created Election");
        request.setDescription("Election created by admin");
        request.setStartTime(LocalDateTime.now().plusMinutes(30));
        request.setEndTime(LocalDateTime.now().plusHours(2));
        
        List<ElectionRequest.CandidateRequest> candidateRequests = List.of(
            new ElectionRequest.CandidateRequest("Candidate A", "First candidate", 1),
            new ElectionRequest.CandidateRequest("Candidate B", "Second candidate", 2),
            new ElectionRequest.CandidateRequest("Candidate C", "Third candidate", 3)
        );
        request.setCandidates(candidateRequests);

        // Create election
        Election createdElection = adminService.createElection(request, adminUser.getUserId());
        assertNotNull(createdElection);
        assertEquals(request.getTitle(), createdElection.getTitle());
        assertEquals(ElectionStatus.DRAFT, createdElection.getStatus());

        // Verify candidates were created
        List<Candidate> candidates = candidateRepository.findByElectionIdOrderByDisplayOrderAsc(createdElection.getElectionId());
        assertEquals(3, candidates.size());
    }

    @Test
    void testEndToEndVotingAndTallying() {
        // Create multiple users and have them vote
        User user2 = createTestUser("user-2", "user2", "user2@example.com");
        User user3 = createTestUser("user-3", "user3", "user3@example.com");

        // User 1 votes for candidate 1
        authenticateAndVote(testUser, testCandidate1);

        // User 2 votes for candidate 1
        authenticateAndVote(user2, testCandidate1);

        // User 3 votes for candidate 2
        authenticateAndVote(user3, testCandidate2);

        // Tally votes
        ElectionResult result = adminService.tallyVotes(testElection.getElectionId());
        assertNotNull(result);
        assertEquals(3, result.getTotalVotes());
        
        // Verify vote counts
        assertEquals(2, result.getCandidateResults().get(testCandidate1.getCandidateId()).getVoteCount());
        assertEquals(1, result.getCandidateResults().get(testCandidate2.getCandidateId()).getVoteCount());
    }

    private User createTestUser(String userId, String username, String email) {
        User user = new User();
        user.setUserId(userId);
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash("$2a$10$hashedpassword");
        user.setQrCodeSecret("qr-secret-" + userId);
        user.setCreatedAt(LocalDateTime.now());
        user.setEnabled(true);
        user.setRole(UserRole.VOTER);
        return userRepository.save(user);
    }

    private void authenticateAndVote(User user, Candidate candidate) {
        // Generate and validate QR code
        String qrData = qrCodeService.generateQRCodeData(user.getUserId());
        authenticationService.authenticateWithQR(user.getUserId(), qrData);

        // Generate and validate OTP
        String otp = otpService.generateOTP(user.getUserId());
        authenticationService.authenticateWithOTP(user.getUserId(), otp);

        // Cast vote
        VoteResult result = votingService.castVote(
            user.getUserId(), 
            candidate.getCandidateId(), 
            testElection.getElectionId(),
            "127.0.0.1",
            "Test-User-Agent"
        );
        assertTrue(result.isSuccessful());
    }
}