package com.securevoting.service;

import com.securevoting.model.*;
import com.securevoting.repository.CandidateRepository;
import com.securevoting.repository.ElectionRepository;
import com.securevoting.repository.VoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for VotingServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class VotingServiceImplTest {
    
    @Mock
    private VoteRepository voteRepository;
    
    @Mock
    private ElectionRepository electionRepository;
    
    @Mock
    private CandidateRepository candidateRepository;
    
    @Mock
    private CryptographyService cryptographyService;
    
    @InjectMocks
    private VotingServiceImpl votingService;
    
    private Election activeElection;
    private Candidate validCandidate;
    private EncryptedVote encryptedVote;
    
    @BeforeEach
    void setUp() {
        // Set up test data
        activeElection = new Election();
        activeElection.setElectionId("election-1");
        activeElection.setTitle("Test Election");
        activeElection.setStatus(ElectionStatus.ACTIVE);
        activeElection.setStartTime(LocalDateTime.now().minusHours(1));
        activeElection.setEndTime(LocalDateTime.now().plusHours(1));
        
        validCandidate = new Candidate();
        validCandidate.setCandidateId("candidate-1");
        validCandidate.setName("Test Candidate");
        validCandidate.setElectionId("election-1");
        validCandidate.setEnabled(true);
        
        encryptedVote = new EncryptedVote();
        encryptedVote.setEncryptedData("encrypted-data");
        encryptedVote.setHash("vote-hash");
        encryptedVote.setInitializationVector("iv-data");
    }
    
    @Test
    void castVote_Success() {
        // Arrange
        String userId = "user-1";
        String candidateId = "candidate-1";
        String electionId = "election-1";
        String ipAddress = "192.168.1.1";
        String userAgent = "Test Agent";
        
        when(voteRepository.existsByUserIdAndElectionId(userId, electionId)).thenReturn(false);
        when(electionRepository.findById(electionId)).thenReturn(Optional.of(activeElection));
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(validCandidate));
        when(cryptographyService.encryptVote(anyString(), eq(electionId))).thenReturn(encryptedVote);
        when(voteRepository.save(any(Vote.class))).thenAnswer(invocation -> {
            Vote vote = invocation.getArgument(0);
            vote.setVoteId("vote-1");
            return vote;
        });
        
        // Act
        VoteResult result = votingService.castVote(userId, candidateId, electionId, ipAddress, userAgent);
        
        // Assert
        assertTrue(result.isSuccessful());
        assertEquals("vote-1", result.getVoteId());
        assertEquals("Your vote has been cast successfully", result.getMessage());
        
        verify(voteRepository).save(any(Vote.class));
        verify(cryptographyService).encryptVote(anyString(), eq(electionId));
    }
    
    @Test
    void castVote_DuplicateVote() {
        // Arrange
        String userId = "user-1";
        String candidateId = "candidate-1";
        String electionId = "election-1";
        
        when(voteRepository.existsByUserIdAndElectionId(userId, electionId)).thenReturn(true);
        
        // Act
        VoteResult result = votingService.castVote(userId, candidateId, electionId, "192.168.1.1", "Test Agent");
        
        // Assert
        assertFalse(result.isSuccessful());
        assertEquals("You have already voted in this election", result.getMessage());
        assertEquals("DUPLICATE_VOTE", result.getErrorCode());
        
        verify(voteRepository, never()).save(any(Vote.class));
    }
    
    @Test
    void castVote_InvalidUser() {
        // Act
        VoteResult result = votingService.castVote(null, "candidate-1", "election-1", "192.168.1.1", "Test Agent");
        
        // Assert
        assertFalse(result.isSuccessful());
        assertEquals("User ID is required", result.getMessage());
        assertEquals("INVALID_USER", result.getErrorCode());
    }
    
    @Test
    void castVote_InvalidCandidate() {
        // Act
        VoteResult result = votingService.castVote("user-1", null, "election-1", "192.168.1.1", "Test Agent");
        
        // Assert
        assertFalse(result.isSuccessful());
        assertEquals("Candidate ID is required", result.getMessage());
        assertEquals("INVALID_CANDIDATE", result.getErrorCode());
    }
    
    @Test
    void castVote_InvalidElection() {
        // Act
        VoteResult result = votingService.castVote("user-1", "candidate-1", "", "192.168.1.1", "Test Agent");
        
        // Assert
        assertFalse(result.isSuccessful());
        assertEquals("Election ID is required", result.getMessage());
        assertEquals("INVALID_ELECTION", result.getErrorCode());
    }
    
    @Test
    void castVote_InactiveElection() {
        // Arrange
        String userId = "user-1";
        String candidateId = "candidate-1";
        String electionId = "election-1";
        
        Election inactiveElection = new Election();
        inactiveElection.setElectionId(electionId);
        inactiveElection.setStatus(ElectionStatus.DRAFT);
        
        when(voteRepository.existsByUserIdAndElectionId(userId, electionId)).thenReturn(false);
        when(electionRepository.findById(electionId)).thenReturn(Optional.of(inactiveElection));
        
        // Act
        VoteResult result = votingService.castVote(userId, candidateId, electionId, "192.168.1.1", "Test Agent");
        
        // Assert
        assertFalse(result.isSuccessful());
        assertEquals("Election is not currently active", result.getMessage());
        assertEquals("ELECTION_INACTIVE", result.getErrorCode());
    }
    
    @Test
    void castVote_CandidateNotInElection() {
        // Arrange
        String userId = "user-1";
        String candidateId = "candidate-1";
        String electionId = "election-1";
        
        Candidate wrongElectionCandidate = new Candidate();
        wrongElectionCandidate.setCandidateId(candidateId);
        wrongElectionCandidate.setElectionId("different-election");
        wrongElectionCandidate.setEnabled(true);
        
        when(voteRepository.existsByUserIdAndElectionId(userId, electionId)).thenReturn(false);
        when(electionRepository.findById(electionId)).thenReturn(Optional.of(activeElection));
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(wrongElectionCandidate));
        
        // Act
        VoteResult result = votingService.castVote(userId, candidateId, electionId, "192.168.1.1", "Test Agent");
        
        // Assert
        assertFalse(result.isSuccessful());
        assertEquals("Invalid candidate for this election", result.getMessage());
        assertEquals("INVALID_CANDIDATE", result.getErrorCode());
    }
    
    @Test
    void hasUserVoted_True() {
        // Arrange
        String userId = "user-1";
        String electionId = "election-1";
        
        when(voteRepository.existsByUserIdAndElectionId(userId, electionId)).thenReturn(true);
        
        // Act
        boolean result = votingService.hasUserVoted(userId, electionId);
        
        // Assert
        assertTrue(result);
    }
    
    @Test
    void hasUserVoted_False() {
        // Arrange
        String userId = "user-1";
        String electionId = "election-1";
        
        when(voteRepository.existsByUserIdAndElectionId(userId, electionId)).thenReturn(false);
        
        // Act
        boolean result = votingService.hasUserVoted(userId, electionId);
        
        // Assert
        assertFalse(result);
    }
    
    @Test
    void hasUserVoted_NullParameters() {
        // Act & Assert
        assertFalse(votingService.hasUserVoted(null, "election-1"));
        assertFalse(votingService.hasUserVoted("user-1", null));
    }
    
    @Test
    void getActiveElections_Success() {
        // Arrange
        List<Election> activeElections = Arrays.asList(activeElection);
        when(electionRepository.findActiveElections(any(LocalDateTime.class))).thenReturn(activeElections);
        
        // Act
        List<Election> result = votingService.getActiveElections();
        
        // Assert
        assertEquals(1, result.size());
        assertEquals(activeElection.getElectionId(), result.get(0).getElectionId());
    }
    
    @Test
    void getElectionDetails_Found() {
        // Arrange
        String electionId = "election-1";
        when(electionRepository.findById(electionId)).thenReturn(Optional.of(activeElection));
        
        // Act
        Election result = votingService.getElectionDetails(electionId);
        
        // Assert
        assertNotNull(result);
        assertEquals(electionId, result.getElectionId());
    }
    
    @Test
    void getElectionDetails_NotFound() {
        // Arrange
        String electionId = "nonexistent-election";
        when(electionRepository.findById(electionId)).thenReturn(Optional.empty());
        
        // Act
        Election result = votingService.getElectionDetails(electionId);
        
        // Assert
        assertNull(result);
    }
    
    @Test
    void getElectionDetails_NullId() {
        // Act
        Election result = votingService.getElectionDetails(null);
        
        // Assert
        assertNull(result);
    }
    
    @Test
    void isElectionActive_True() {
        // Arrange
        String electionId = "election-1";
        when(electionRepository.findById(electionId)).thenReturn(Optional.of(activeElection));
        
        // Act
        boolean result = votingService.isElectionActive(electionId);
        
        // Assert
        assertTrue(result);
    }
    
    @Test
    void isElectionActive_False() {
        // Arrange
        String electionId = "election-1";
        Election inactiveElection = new Election();
        inactiveElection.setElectionId(electionId);
        inactiveElection.setStatus(ElectionStatus.DRAFT);
        
        when(electionRepository.findById(electionId)).thenReturn(Optional.of(inactiveElection));
        
        // Act
        boolean result = votingService.isElectionActive(electionId);
        
        // Assert
        assertFalse(result);
    }
    
    @Test
    void isElectionActive_NotFound() {
        // Arrange
        String electionId = "nonexistent-election";
        when(electionRepository.findById(electionId)).thenReturn(Optional.empty());
        
        // Act
        boolean result = votingService.isElectionActive(electionId);
        
        // Assert
        assertFalse(result);
    }
    
    @Test
    void isCandidateValid_True() {
        // Arrange
        String candidateId = "candidate-1";
        String electionId = "election-1";
        
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(validCandidate));
        
        // Act
        boolean result = votingService.isCandidateValid(candidateId, electionId);
        
        // Assert
        assertTrue(result);
    }
    
    @Test
    void isCandidateValid_WrongElection() {
        // Arrange
        String candidateId = "candidate-1";
        String electionId = "different-election";
        
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(validCandidate));
        
        // Act
        boolean result = votingService.isCandidateValid(candidateId, electionId);
        
        // Assert
        assertFalse(result);
    }
    
    @Test
    void isCandidateValid_Disabled() {
        // Arrange
        String candidateId = "candidate-1";
        String electionId = "election-1";
        
        Candidate disabledCandidate = new Candidate();
        disabledCandidate.setCandidateId(candidateId);
        disabledCandidate.setElectionId(electionId);
        disabledCandidate.setEnabled(false);
        
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(disabledCandidate));
        
        // Act
        boolean result = votingService.isCandidateValid(candidateId, electionId);
        
        // Assert
        assertFalse(result);
    }
    
    @Test
    void isCandidateValid_NotFound() {
        // Arrange
        String candidateId = "nonexistent-candidate";
        String electionId = "election-1";
        
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.empty());
        
        // Act
        boolean result = votingService.isCandidateValid(candidateId, electionId);
        
        // Assert
        assertFalse(result);
    }
    
    @Test
    void getUserVote_Found() {
        // Arrange
        String userId = "user-1";
        String electionId = "election-1";
        
        Vote vote = new Vote();
        vote.setVoteId("vote-1");
        vote.setUserId(userId);
        vote.setElectionId(electionId);
        
        when(voteRepository.findByUserIdAndElectionId(userId, electionId)).thenReturn(Optional.of(vote));
        
        // Act
        Vote result = votingService.getUserVote(userId, electionId);
        
        // Assert
        assertNotNull(result);
        assertEquals("vote-1", result.getVoteId());
    }
    
    @Test
    void getUserVote_NotFound() {
        // Arrange
        String userId = "user-1";
        String electionId = "election-1";
        
        when(voteRepository.findByUserIdAndElectionId(userId, electionId)).thenReturn(Optional.empty());
        
        // Act
        Vote result = votingService.getUserVote(userId, electionId);
        
        // Assert
        assertNull(result);
    }
    
    @Test
    void getVoteCount_Success() {
        // Arrange
        String electionId = "election-1";
        long expectedCount = 5L;
        
        when(voteRepository.countByElectionId(electionId)).thenReturn(expectedCount);
        
        // Act
        long result = votingService.getVoteCount(electionId);
        
        // Assert
        assertEquals(expectedCount, result);
    }
    
    @Test
    void getVoteCount_NullElectionId() {
        // Act
        long result = votingService.getVoteCount(null);
        
        // Assert
        assertEquals(0, result);
    }
}