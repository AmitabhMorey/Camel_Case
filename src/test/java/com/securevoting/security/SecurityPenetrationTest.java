package com.securevoting.security;

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
 * Security tests for authentication bypass attempts and data tampering
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class SecurityPenetrationTest {

    @Autowired
    private AuthenticationService authenticationService;
    
    @Autowired
    private VotingService votingService;
    
    @Autowired
    private QRCodeService qrCodeService;
    
    @Autowired
    private OTPService otpService;
    
    @Autowired
    private CryptographyService cryptographyService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ElectionRepository electionRepository;
    
    @Autowired
    private CandidateRepository candidateRepository;
    
    @Autowired
    private VoteRepository voteRepository;
    
    @Autowired
    private AuditLogRepository auditLogRepository;

    private User testUser;
    private Election testElection;
    private Candidate testCandidate;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setUserId("security-test-user");
        testUser.setUsername("securityuser");
        testUser.setEmail("security@example.com");
        testUser.setPasswordHash("$2a$10$hashedpassword");
        testUser.setQrCodeSecret("security-qr-secret");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setEnabled(true);
        testUser.setRole(UserRole.VOTER);
        userRepository.save(testUser);

        // Create test election
        testElection = new Election();
        testElection.setElectionId("security-election");
        testElection.setTitle("Security Test Election");
        testElection.setDescription("Election for security testing");
        testElection.setStartTime(LocalDateTime.now().minusHours(1));
        testElection.setEndTime(LocalDateTime.now().plusHours(1));
        testElection.setStatus(ElectionStatus.ACTIVE);
        electionRepository.save(testElection);

        // Create test candidate
        testCandidate = new Candidate();
        testCandidate.setCandidateId("security-candidate");
        testCandidate.setName("Security Candidate");
        testCandidate.setDescription("Candidate for security testing");
        testCandidate.setElectionId(testElection.getElectionId());
        candidateRepository.save(testCandidate);
    }

    @Test
    void testQRCodeBypassAttempts() {
        // Test 1: Invalid QR code format
        AuthenticationResult result1 = authenticationService.authenticateWithQR(
            testUser.getUserId(), "invalid-qr-format");
        assertFalse(result1.isSuccessful());
        assertNotNull(result1.getErrorMessage());

        // Test 2: QR code for different user
        User otherUser = createTestUser("other-user", "otheruser", "other@example.com");
        String validQrForOther = qrCodeService.generateQRCodeData(otherUser.getUserId());
        
        AuthenticationResult result2 = authenticationService.authenticateWithQR(
            testUser.getUserId(), validQrForOther);
        assertFalse(result2.isSuccessful());

        // Test 3: Expired QR code simulation (manipulated timestamp)
        String expiredQr = "expired-qr-data-" + (System.currentTimeMillis() - 600000); // 10 minutes old
        AuthenticationResult result3 = authenticationService.authenticateWithQR(
            testUser.getUserId(), expiredQr);
        assertFalse(result3.isSuccessful());

        // Test 4: Malformed QR data
        String[] malformedQrCodes = {
            "",
            "null",
            "undefined",
            "<script>alert('xss')</script>",
            "'; DROP TABLE users; --",
            "../../../etc/passwd",
            "%00%00%00%00"
        };

        for (String malformedQr : malformedQrCodes) {
            AuthenticationResult result = authenticationService.authenticateWithQR(
                testUser.getUserId(), malformedQr);
            assertFalse(result.isSuccessful(), "Malformed QR should fail: " + malformedQr);
        }

        // Verify security events were logged
        List<AuditLog> securityLogs = auditLogRepository.findByEventType(AuditEventType.SECURITY_EVENT);
        assertFalse(securityLogs.isEmpty());
    }

    @Test
    void testOTPBypassAttempts() {
        // Generate valid OTP first
        String validOtp = otpService.generateOTP(testUser.getUserId());

        // Test 1: Invalid OTP formats
        String[] invalidOtps = {
            "123456", // Wrong OTP
            "000000", // All zeros
            "999999", // All nines
            "12345",  // Too short
            "1234567", // Too long
            "abcdef", // Non-numeric
            "",       // Empty
            "null",   // String null
            "<script>", // XSS attempt
            "'; DROP TABLE users; --" // SQL injection attempt
        };

        for (String invalidOtp : invalidOtps) {
            AuthenticationResult result = authenticationService.authenticateWithOTP(
                testUser.getUserId(), invalidOtp);
            assertFalse(result.isSuccessful(), "Invalid OTP should fail: " + invalidOtp);
        }

        // Test 2: OTP reuse attempt
        AuthenticationResult validResult = authenticationService.authenticateWithOTP(
            testUser.getUserId(), validOtp);
        assertTrue(validResult.isSuccessful());

        // Try to reuse the same OTP
        AuthenticationResult reuseResult = authenticationService.authenticateWithOTP(
            testUser.getUserId(), validOtp);
        assertFalse(reuseResult.isSuccessful());

        // Test 3: OTP for different user
        User otherUser = createTestUser("otp-other-user", "otpother", "otpother@example.com");
        String otpForOther = otpService.generateOTP(otherUser.getUserId());
        
        AuthenticationResult crossUserResult = authenticationService.authenticateWithOTP(
            testUser.getUserId(), otpForOther);
        assertFalse(crossUserResult.isSuccessful());
    }

    @Test
    void testVoteDataTamperingAttempts() {
        // Authenticate user first
        authenticateUser(testUser);

        // Test 1: Vote with invalid candidate ID
        VoteResult result1 = votingService.castVote(
            testUser.getUserId(), "non-existent-candidate", testElection.getElectionId(), "127.0.0.1", "Test-Agent");
        assertFalse(result1.isSuccessful());

        // Test 2: Vote with invalid election ID
        VoteResult result2 = votingService.castVote(
            testUser.getUserId(), testCandidate.getCandidateId(), "non-existent-election", "127.0.0.1", "Test-Agent");
        assertFalse(result2.isSuccessful());

        // Test 3: SQL injection attempts in vote data
        String[] maliciousInputs = {
            "'; DROP TABLE votes; --",
            "1' OR '1'='1",
            "<script>alert('xss')</script>",
            "../../../etc/passwd",
            "%00%00%00%00",
            "UNION SELECT * FROM users"
        };

        for (String maliciousInput : maliciousInputs) {
            VoteResult result = votingService.castVote(
                testUser.getUserId(), maliciousInput, testElection.getElectionId(), "127.0.0.1", "Test-Agent");
            assertFalse(result.isSuccessful(), "Malicious input should fail: " + maliciousInput);
        }
    }

    @Test
    void testEncryptionTamperingDetection() {
        // Create a valid encrypted vote
        String originalVoteData = "candidate-" + testCandidate.getCandidateId();
        EncryptedVote encryptedVote = cryptographyService.encryptVote(originalVoteData, testElection.getElectionId());

        // Test 1: Tamper with encrypted data
        EncryptedVote tamperedData = new EncryptedVote();
        tamperedData.setEncryptedData("tampered-data");
        tamperedData.setInitializationVector(encryptedVote.getInitializationVector());
        tamperedData.setHash(encryptedVote.getHash());
        tamperedData.setAlgorithm(encryptedVote.getAlgorithm());

        assertThrows(Exception.class, () -> {
            cryptographyService.decryptVote(tamperedData);
        });

        // Test 2: Tamper with hash
        EncryptedVote tamperedHash = new EncryptedVote();
        tamperedHash.setEncryptedData(encryptedVote.getEncryptedData());
        tamperedHash.setInitializationVector(encryptedVote.getInitializationVector());
        tamperedHash.setHash("tampered-hash");
        tamperedHash.setAlgorithm(encryptedVote.getAlgorithm());

        // Hash verification should fail
        String decryptedData = cryptographyService.decryptVote(tamperedHash);
        boolean hashValid = cryptographyService.verifyHash(decryptedData, tamperedHash.getHash());
        assertFalse(hashValid);

        // Test 3: Tamper with initialization vector
        EncryptedVote tamperedIV = new EncryptedVote();
        tamperedIV.setEncryptedData(encryptedVote.getEncryptedData());
        tamperedIV.setInitializationVector("tampered-iv-123456");
        tamperedIV.setHash(encryptedVote.getHash());
        tamperedIV.setAlgorithm(encryptedVote.getAlgorithm());

        assertThrows(Exception.class, () -> {
            cryptographyService.decryptVote(tamperedIV);
        });
    }

    @Test
    void testSessionHijackingPrevention() {
        // Authenticate user and get session
        authenticateUser(testUser);

        // Test 1: Invalid session ID
        String[] invalidSessionIds = {
            "invalid-session",
            "",
            "null",
            "<script>alert('xss')</script>",
            "'; DROP TABLE sessions; --",
            "../../../etc/passwd"
        };

        for (String invalidSessionId : invalidSessionIds) {
            // Attempt to use invalid session (this would be tested at controller level)
            // For now, we verify that authentication service doesn't accept invalid sessions
            assertThrows(Exception.class, () -> {
                authenticationService.invalidateSession(invalidSessionId);
            });
        }
    }

    @Test
    void testRateLimitingBypass() {
        // Test rapid authentication attempts
        int maxAttempts = 10;
        int successfulAttempts = 0;
        int failedAttempts = 0;

        for (int i = 0; i < maxAttempts; i++) {
            try {
                AuthenticationResult result = authenticationService.authenticateWithQR(
                    testUser.getUserId(), "invalid-qr-" + i);
                if (result.isSuccessful()) {
                    successfulAttempts++;
                } else {
                    failedAttempts++;
                }
            } catch (Exception e) {
                // Rate limiting might throw exceptions
                failedAttempts++;
            }
        }

        // All attempts should fail due to invalid QR codes
        assertEquals(0, successfulAttempts);
        assertEquals(maxAttempts, failedAttempts);

        // Verify rate limiting logs
        List<AuditLog> rateLimitLogs = auditLogRepository.findByEventType(AuditEventType.SECURITY_EVENT);
        assertFalse(rateLimitLogs.isEmpty());
    }

    @Test
    void testDatabaseInjectionPrevention() {
        // Test SQL injection in user lookup
        String[] sqlInjectionAttempts = {
            "'; DROP TABLE users; --",
            "' OR '1'='1",
            "' UNION SELECT * FROM votes --",
            "admin'--",
            "' OR 1=1 --"
        };

        for (String injection : sqlInjectionAttempts) {
            // These should not cause database errors or return unauthorized data
            AuthenticationResult result = authenticationService.authenticateWithQR(injection, "test-qr");
            assertFalse(result.isSuccessful());

            VoteResult voteResult = votingService.castVote(injection, testCandidate.getCandidateId(), testElection.getElectionId(), "127.0.0.1", "Test-Agent");
            assertFalse(voteResult.isSuccessful());
        }
    }

    @Test
    void testCrossUserDataAccess() {
        // Create another user
        User otherUser = createTestUser("cross-user-test", "crossuser", "cross@example.com");
        
        // Authenticate and vote as first user
        authenticateUser(testUser);
        VoteResult vote1 = votingService.castVote(
            testUser.getUserId(), testCandidate.getCandidateId(), testElection.getElectionId(), "127.0.0.1", "Test-Agent");
        assertTrue(vote1.isSuccessful());

        // Try to access first user's vote data as second user
        authenticateUser(otherUser);
        
        // Attempt to vote as the other user using first user's ID (should fail)
        VoteResult crossVote = votingService.castVote(
            testUser.getUserId(), testCandidate.getCandidateId(), testElection.getElectionId(), "127.0.0.1", "Test-Agent");
        assertFalse(crossVote.isSuccessful());

        // Verify vote isolation
        boolean user1Voted = votingService.hasUserVoted(testUser.getUserId(), testElection.getElectionId());
        boolean user2Voted = votingService.hasUserVoted(otherUser.getUserId(), testElection.getElectionId());
        
        assertTrue(user1Voted);
        assertFalse(user2Voted);
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

    private void authenticateUser(User user) {
        String qrData = qrCodeService.generateQRCodeData(user.getUserId());
        AuthenticationResult qrResult = authenticationService.authenticateWithQR(user.getUserId(), qrData);
        assertTrue(qrResult.isSuccessful());

        String otp = otpService.generateOTP(user.getUserId());
        AuthenticationResult otpResult = authenticationService.authenticateWithOTP(user.getUserId(), otp);
        assertTrue(otpResult.isSuccessful());
    }
}