package com.securevoting.performance;

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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance tests for concurrent voting scenarios and encryption operations
 */
@SpringBootTest
@ActiveProfiles("test")
public class ConcurrentVotingPerformanceTest {

    @Autowired
    private VotingService votingService;
    
    @Autowired
    private AuthenticationService authenticationService;
    
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

    private Election testElection;
    private Candidate testCandidate1;
    private Candidate testCandidate2;
    private List<User> testUsers;

    @BeforeEach
    void setUp() {
        // Create test election
        testElection = new Election();
        testElection.setElectionId("perf-election-" + System.currentTimeMillis());
        testElection.setTitle("Performance Test Election");
        testElection.setDescription("Election for performance testing");
        testElection.setStartTime(LocalDateTime.now().minusHours(1));
        testElection.setEndTime(LocalDateTime.now().plusHours(1));
        testElection.setStatus(ElectionStatus.ACTIVE);
        electionRepository.save(testElection);

        // Create test candidates
        testCandidate1 = new Candidate();
        testCandidate1.setCandidateId("perf-candidate-1-" + System.currentTimeMillis());
        testCandidate1.setName("Performance Candidate 1");
        testCandidate1.setDescription("First performance test candidate");
        testCandidate1.setElectionId(testElection.getElectionId());
        candidateRepository.save(testCandidate1);

        testCandidate2 = new Candidate();
        testCandidate2.setCandidateId("perf-candidate-2-" + System.currentTimeMillis());
        testCandidate2.setName("Performance Candidate 2");
        testCandidate2.setDescription("Second performance test candidate");
        testCandidate2.setElectionId(testElection.getElectionId());
        candidateRepository.save(testCandidate2);

        // Create test users
        testUsers = createTestUsers(50);
    }

    @Test
    void testConcurrentVotingPerformance() throws InterruptedException {
        int numberOfThreads = 10;
        int votesPerThread = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger successfulVotes = new AtomicInteger(0);
        AtomicInteger failedVotes = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();

        // Submit voting tasks
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadIndex = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < votesPerThread; j++) {
                        int userIndex = threadIndex * votesPerThread + j;
                        if (userIndex < testUsers.size()) {
                            User user = testUsers.get(userIndex);
                            Candidate candidate = (j % 2 == 0) ? testCandidate1 : testCandidate2;
                            
                            if (performVotingWorkflow(user, candidate)) {
                                successfulVotes.incrementAndGet();
                            } else {
                                failedVotes.incrementAndGet();
                            }
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for all threads to complete
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        assertTrue(completed, "Concurrent voting test did not complete within timeout");
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        executor.shutdown();

        // Verify results
        int expectedVotes = Math.min(numberOfThreads * votesPerThread, testUsers.size());
        assertEquals(expectedVotes, successfulVotes.get());
        assertEquals(0, failedVotes.get());

        // Performance assertions
        assertTrue(duration < 15000, "Concurrent voting took too long: " + duration + "ms");
        
        System.out.println("Concurrent Voting Performance Results:");
        System.out.println("Total votes: " + successfulVotes.get());
        System.out.println("Duration: " + duration + "ms");
        System.out.println("Votes per second: " + (successfulVotes.get() * 1000.0 / duration));
    }

    @Test
    void testEncryptionPerformance() {
        int numberOfOperations = 1000;
        List<String> testData = new ArrayList<>();
        
        // Generate test vote data
        for (int i = 0; i < numberOfOperations; i++) {
            testData.add("vote-data-" + i + "-candidate-" + (i % 2 == 0 ? testCandidate1.getCandidateId() : testCandidate2.getCandidateId()));
        }

        long startTime = System.currentTimeMillis();

        // Perform encryption operations
        List<EncryptedVote> encryptedVotes = new ArrayList<>();
        for (String data : testData) {
            EncryptedVote encrypted = cryptographyService.encryptVote(data, testElection.getElectionId());
            encryptedVotes.add(encrypted);
        }

        long encryptionTime = System.currentTimeMillis();

        // Perform decryption operations
        List<String> decryptedVotes = new ArrayList<>();
        for (EncryptedVote encrypted : encryptedVotes) {
            String decrypted = cryptographyService.decryptVote(encrypted);
            decryptedVotes.add(decrypted);
        }

        long decryptionTime = System.currentTimeMillis();

        // Verify data integrity
        assertEquals(numberOfOperations, encryptedVotes.size());
        assertEquals(numberOfOperations, decryptedVotes.size());
        
        for (int i = 0; i < numberOfOperations; i++) {
            assertEquals(testData.get(i), decryptedVotes.get(i));
        }

        // Performance assertions
        long totalEncryptionTime = encryptionTime - startTime;
        long totalDecryptionTime = decryptionTime - encryptionTime;
        
        assertTrue(totalEncryptionTime < 5000, "Encryption took too long: " + totalEncryptionTime + "ms");
        assertTrue(totalDecryptionTime < 5000, "Decryption took too long: " + totalDecryptionTime + "ms");

        System.out.println("Encryption Performance Results:");
        System.out.println("Encryption operations: " + numberOfOperations);
        System.out.println("Encryption time: " + totalEncryptionTime + "ms");
        System.out.println("Encryptions per second: " + (numberOfOperations * 1000.0 / totalEncryptionTime));
        System.out.println("Decryption time: " + totalDecryptionTime + "ms");
        System.out.println("Decryptions per second: " + (numberOfOperations * 1000.0 / totalDecryptionTime));
    }

    @Test
    void testConcurrentAuthenticationPerformance() throws InterruptedException {
        int numberOfThreads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger successfulAuthentications = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();

        // Submit authentication tasks
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadIndex = i;
            executor.submit(() -> {
                try {
                    if (threadIndex < testUsers.size()) {
                        User user = testUsers.get(threadIndex);
                        
                        // QR authentication
                        String qrData = qrCodeService.generateQRCodeData(user.getUserId());
                        AuthenticationResult qrResult = authenticationService.authenticateWithQR(
                            user.getUserId(), qrData);
                        
                        if (qrResult.isSuccessful()) {
                            // OTP authentication
                            String otp = otpService.generateOTP(user.getUserId());
                            AuthenticationResult otpResult = authenticationService.authenticateWithOTP(
                                user.getUserId(), otp);
                            
                            if (otpResult.isSuccessful()) {
                                successfulAuthentications.incrementAndGet();
                            }
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for all threads to complete
        boolean completed = latch.await(20, TimeUnit.SECONDS);
        assertTrue(completed, "Concurrent authentication test did not complete within timeout");
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        executor.shutdown();

        // Verify results
        int expectedAuthentications = Math.min(numberOfThreads, testUsers.size());
        assertEquals(expectedAuthentications, successfulAuthentications.get());

        // Performance assertions
        assertTrue(duration < 10000, "Concurrent authentication took too long: " + duration + "ms");
        
        System.out.println("Concurrent Authentication Performance Results:");
        System.out.println("Total authentications: " + successfulAuthentications.get());
        System.out.println("Duration: " + duration + "ms");
        System.out.println("Authentications per second: " + (successfulAuthentications.get() * 1000.0 / duration));
    }

    @Test
    void testQRCodeGenerationPerformance() {
        int numberOfOperations = 500;
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numberOfOperations; i++) {
            String userId = "perf-user-" + i;
            String qrData = qrCodeService.generateQRCodeData(userId);
            assertNotNull(qrData);
            assertTrue(qrData.contains(userId));
            
            // Generate QR code image
            byte[] qrImage = qrCodeService.generateQRCodeImage(qrData);
            assertNotNull(qrImage);
            assertTrue(qrImage.length > 0);
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Performance assertions
        assertTrue(duration < 3000, "QR code generation took too long: " + duration + "ms");
        
        System.out.println("QR Code Generation Performance Results:");
        System.out.println("QR codes generated: " + numberOfOperations);
        System.out.println("Duration: " + duration + "ms");
        System.out.println("QR codes per second: " + (numberOfOperations * 1000.0 / duration));
    }

    private List<User> createTestUsers(int count) {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            User user = new User();
            user.setUserId("perf-user-" + i + "-" + System.currentTimeMillis());
            user.setUsername("perfuser" + i);
            user.setEmail("perfuser" + i + "@example.com");
            user.setPasswordHash("$2a$10$hashedpassword");
            user.setQrCodeSecret("qr-secret-" + i);
            user.setCreatedAt(LocalDateTime.now());
            user.setEnabled(true);
            user.setRole(UserRole.VOTER);
            users.add(userRepository.save(user));
        }
        return users;
    }

    private boolean performVotingWorkflow(User user, Candidate candidate) {
        try {
            // QR authentication
            String qrData = qrCodeService.generateQRCodeData(user.getUserId());
            AuthenticationResult qrResult = authenticationService.authenticateWithQR(
                user.getUserId(), qrData);
            
            if (!qrResult.isSuccessful()) {
                return false;
            }

            // OTP authentication
            String otp = otpService.generateOTP(user.getUserId());
            AuthenticationResult otpResult = authenticationService.authenticateWithOTP(
                user.getUserId(), otp);
            
            if (!otpResult.isSuccessful()) {
                return false;
            }

            // Cast vote
            VoteResult voteResult = votingService.castVote(
                user.getUserId(), 
                candidate.getCandidateId(), 
                testElection.getElectionId(),
                "127.0.0.1",
                "Test-User-Agent"
            );
            
            return voteResult.isSuccessful();
        } catch (Exception e) {
            System.err.println("Error in voting workflow for user " + user.getUserId() + ": " + e.getMessage());
            return false;
        }
    }
}