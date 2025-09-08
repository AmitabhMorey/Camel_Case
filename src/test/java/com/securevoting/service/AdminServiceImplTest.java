package com.securevoting.service;

import com.securevoting.model.*;
import com.securevoting.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AdminServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {
    
    @Mock
    private ElectionRepository electionRepository;
    
    @Mock
    private CandidateRepository candidateRepository;
    
    @Mock
    private VoteRepository voteRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private AuditLogRepository auditLogRepository;
    
    @Mock
    private CryptographyService cryptographyService;
    
    @Mock
    private AuditService auditService;
    
    @InjectMocks
    private AdminServiceImpl adminService;
    
    private ElectionRequest validElectionRequest;
    private Election testElection;
    private List<Candidate> testCandidates;
    private Vote testVote;
    private EncryptedVote encryptedVote;
    
    @BeforeEach
    void setUp() {
        // Set up test election request
        ElectionRequest.CandidateRequest candidate1 = new ElectionRequest.CandidateRequest(
            "John Doe", "Experienced leader", 1);
        ElectionRequest.CandidateRequest candidate2 = new ElectionRequest.CandidateRequest(
            "Jane Smith", "Fresh perspective", 2);
        
        validElectionRequest = new ElectionRequest(
            "Test Election",
            "A test election for unit testing",
            LocalDateTime.now().plusDays(1),
            LocalDateTime.now().plusDays(7),
            Arrays.asList(candidate1, candidate2)
        );
        
        // Set up test election
        testElection = new Election(
            "election-123",
            "Test Election",
            "A test election",
            LocalDateTime.now().plusDays(1),
            LocalDateTime.now().plusDays(7),
            "admin-123"
        );
        testElection.setStatus(ElectionStatus.DRAFT);
        
        // Set up test candidates
        testCandidates = Arrays.asList(
            new Candidate("candidate-1", "John Doe", "Experienced leader", "election-123"),
            new Candidate("candidate-2", "Jane Smith", "Fresh perspective", "election-123")
        );
        testCandidates.get(0).setDisplayOrder(1);
        testCandidates.get(1).setDisplayOrder(2);
        
        // Set up test vote
        testVote = new Vote(
            "vote-123",
            "user-123",
            "election-123",
            "encrypted-data",
            "vote-hash",
            "iv-123"
        );
        
        // Set up encrypted vote
        encryptedVote = new EncryptedVote(
            "encrypted-data",
            "iv-123",
            "vote-hash",
            "AES-256-GCM"
        );
    }
    
    @Test
    void createElection_ValidRequest_ShouldCreateElection() {
        // Arrange
        when(electionRepository.save(any(Election.class))).thenReturn(testElection);
        when(candidateRepository.saveAll(anyList())).thenReturn(testCandidates);
        
        // Act
        Election result = adminService.createElection(validElectionRequest, "admin-123");
        
        // Assert
        assertNotNull(result);
        assertEquals("Test Election", result.getTitle());
        assertEquals("admin-123", result.getCreatedBy());
        
        verify(electionRepository).save(any(Election.class));
        verify(candidateRepository).saveAll(anyList());
        verify(auditService).logAdminAction(eq("admin-123"), eq("CREATE_ELECTION"), anyString(), anyString());
    }
    
    @Test
    void createElection_InvalidRequest_ShouldThrowException() {
        // Arrange
        ElectionRequest invalidRequest = new ElectionRequest();
        invalidRequest.setTitle(""); // Invalid title
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> adminService.createElection(invalidRequest, "admin-123"));
        
        verify(electionRepository, never()).save(any());
        verify(candidateRepository, never()).saveAll(anyList());
    }
    
    @Test
    void createElection_StartTimeInPast_ShouldThrowException() {
        // Arrange
        validElectionRequest.setStartTime(LocalDateTime.now().minusDays(1));
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> adminService.createElection(validElectionRequest, "admin-123"));
    }
    
    @Test
    void createElection_StartTimeAfterEndTime_ShouldThrowException() {
        // Arrange
        validElectionRequest.setStartTime(LocalDateTime.now().plusDays(7));
        validElectionRequest.setEndTime(LocalDateTime.now().plusDays(1));
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> adminService.createElection(validElectionRequest, "admin-123"));
    }
    
    @Test
    void updateElection_ValidRequest_ShouldUpdateElection() {
        // Arrange
        when(electionRepository.findById("election-123")).thenReturn(Optional.of(testElection));
        when(electionRepository.save(any(Election.class))).thenReturn(testElection);
        when(candidateRepository.saveAll(anyList())).thenReturn(testCandidates);
        
        testElection.setCandidates(testCandidates);
        
        // Act
        Election result = adminService.updateElection("election-123", validElectionRequest, "admin-123");
        
        // Assert
        assertNotNull(result);
        verify(electionRepository).save(any(Election.class));
        verify(auditService).logAdminAction(eq("admin-123"), eq("UPDATE_ELECTION"), anyString(), anyString());
    }
    
    @Test
    void updateElection_ElectionNotFound_ShouldThrowException() {
        // Arrange
        when(electionRepository.findById("election-123")).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> adminService.updateElection("election-123", validElectionRequest, "admin-123"));
    }
    
    @Test
    void updateElection_ElectionAlreadyStarted_ShouldThrowException() {
        // Arrange
        testElection.setStatus(ElectionStatus.ACTIVE);
        testElection.setStartTime(LocalDateTime.now().minusHours(1));
        when(electionRepository.findById("election-123")).thenReturn(Optional.of(testElection));
        
        // Act & Assert
        assertThrows(RuntimeException.class, 
            () -> adminService.updateElection("election-123", validElectionRequest, "admin-123"));
    }
    
    @Test
    void deleteElection_ValidElection_ShouldDeleteElection() {
        // Arrange
        when(electionRepository.findById("election-123")).thenReturn(Optional.of(testElection));
        when(voteRepository.countByElectionId("election-123")).thenReturn(0L);
        testElection.setCandidates(testCandidates);
        
        // Act
        adminService.deleteElection("election-123", "admin-123");
        
        // Assert
        verify(candidateRepository).deleteAll(testCandidates);
        verify(electionRepository).delete(testElection);
        verify(auditService).logAdminAction(eq("admin-123"), eq("DELETE_ELECTION"), anyString(), anyString());
    }
    
    @Test
    void deleteElection_ElectionWithVotes_ShouldThrowException() {
        // Arrange
        when(electionRepository.findById("election-123")).thenReturn(Optional.of(testElection));
        when(voteRepository.countByElectionId("election-123")).thenReturn(5L);
        
        // Act & Assert
        assertThrows(RuntimeException.class, 
            () -> adminService.deleteElection("election-123", "admin-123"));
        
        verify(electionRepository, never()).delete(any());
    }
    
    @Test
    void deleteElection_ActiveElection_ShouldThrowException() {
        // Arrange
        testElection.setStatus(ElectionStatus.ACTIVE);
        when(electionRepository.findById("election-123")).thenReturn(Optional.of(testElection));
        
        // Act & Assert
        assertThrows(RuntimeException.class, 
            () -> adminService.deleteElection("election-123", "admin-123"));
    }
    
    @Test
    void tallyVotes_ValidElection_ShouldReturnResults() {
        // Arrange
        when(electionRepository.findById("election-123")).thenReturn(Optional.of(testElection));
        when(voteRepository.findByElectionIdOrderByTimestampAsc("election-123"))
            .thenReturn(Arrays.asList(testVote));
        when(candidateRepository.findByElectionIdOrderByDisplayOrderAsc("election-123"))
            .thenReturn(testCandidates);
        when(cryptographyService.decryptVote(any(EncryptedVote.class)))
            .thenReturn("candidate-1");
        when(cryptographyService.generateHash("candidate-1")).thenReturn("vote-hash");
        when(cryptographyService.verifyHash("candidate-1", "vote-hash")).thenReturn(true);
        
        // Act
        ElectionResult result = adminService.tallyVotes("election-123");
        
        // Assert
        assertNotNull(result);
        assertEquals("election-123", result.getElectionId());
        assertEquals(1, result.getTotalVotes());
        assertEquals(1, result.getValidVotes());
        assertEquals(0, result.getInvalidVotes());
        assertTrue(result.isTallySuccessful());
        
        verify(auditService).logAdminAction(eq("SYSTEM"), eq("TALLY_VOTES"), anyString(), anyString());
    }
    
    @Test
    void tallyVotes_ElectionNotFound_ShouldThrowException() {
        // Arrange
        when(electionRepository.findById("election-123")).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> adminService.tallyVotes("election-123"));
    }
    
    @Test
    void tallyVotes_DecryptionFailure_ShouldHandleGracefully() {
        // Arrange
        when(electionRepository.findById("election-123")).thenReturn(Optional.of(testElection));
        when(voteRepository.findByElectionIdOrderByTimestampAsc("election-123"))
            .thenReturn(Arrays.asList(testVote));
        when(candidateRepository.findByElectionIdOrderByDisplayOrderAsc("election-123"))
            .thenReturn(testCandidates);
        when(cryptographyService.decryptVote(any(EncryptedVote.class)))
            .thenThrow(new RuntimeException("Decryption failed"));
        
        // Act
        ElectionResult result = adminService.tallyVotes("election-123");
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalVotes());
        assertEquals(0, result.getValidVotes());
        assertEquals(1, result.getInvalidVotes());
        assertFalse(result.isTallySuccessful());
        assertFalse(result.getIntegrityIssues().isEmpty());
    }
    
    @Test
    void getAuditLogs_NoFilter_ShouldReturnRecentLogs() {
        // Arrange
        List<AuditLog> mockLogs = Arrays.asList(
            createMockAuditLog("log-1", "user-1", "LOGIN", AuditEventType.AUTH_SUCCESS, "127.0.0.1"),
            createMockAuditLog("log-2", "user-2", "VOTE", AuditEventType.VOTE_CAST, "127.0.0.2")
        );
        Page<AuditLog> mockPage = new PageImpl<>(mockLogs);
        when(auditLogRepository.findAllByOrderByTimestampDesc(any(PageRequest.class)))
            .thenReturn(mockPage);
        
        // Act
        List<AuditLog> result = adminService.getAuditLogs(null);
        
        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(auditLogRepository).findAllByOrderByTimestampDesc(any(PageRequest.class));
    }
    
    @Test
    void getAuditLogs_WithFilter_ShouldUseFilteredQuery() {
        // Arrange
        AuditFilter filter = new AuditFilter();
        filter.setUserId("user-123");
        filter.setEventType(AuditEventType.AUTH_SUCCESS);
        
        List<AuditLog> mockLogs = Arrays.asList(
            createMockAuditLog("log-1", "user-123", "LOGIN", AuditEventType.AUTH_SUCCESS, "127.0.0.1")
        );
        when(auditLogRepository.findByFilters(eq("user-123"), eq(AuditEventType.AUTH_SUCCESS), 
            isNull(), isNull(), isNull())).thenReturn(mockLogs);
        
        // Act
        List<AuditLog> result = adminService.getAuditLogs(filter);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(auditLogRepository).findByFilters(eq("user-123"), eq(AuditEventType.AUTH_SUCCESS), 
            isNull(), isNull(), isNull());
    }
    
    @Test
    void getSystemStatistics_ShouldReturnComprehensiveStats() {
        // Arrange
        when(userRepository.count()).thenReturn(100L);
        when(electionRepository.count()).thenReturn(10L);
        when(electionRepository.findActiveElections(any())).thenReturn(Arrays.asList(testElection));
        when(electionRepository.findCompletedElections(any())).thenReturn(Arrays.asList());
        when(voteRepository.count()).thenReturn(500L);
        when(voteRepository.findVotesInTimeRange(any(), any())).thenReturn(Arrays.asList(testVote));
        when(auditLogRepository.findLogsInTimeRange(any(), any())).thenReturn(Arrays.asList());
        when(electionRepository.countByStatus(ElectionStatus.DRAFT)).thenReturn(5L);

        
        // Act
        SystemStatistics result = adminService.getSystemStatistics();
        
        // Assert
        assertNotNull(result);
        assertEquals(100, result.getTotalUsers());
        assertEquals(10, result.getTotalElections());
        assertEquals(1, result.getActiveElections());
        assertEquals(0, result.getCompletedElections());
        assertEquals(500, result.getTotalVotes());
        assertEquals(1, result.getVotesToday());
        assertNotNull(result.getElectionStatusCounts());
        assertNotNull(result.getRecentActivityCounts());
    }
    
    @Test
    void getAllElections_NoFilter_ShouldReturnAllElections() {
        // Arrange
        List<Election> mockElections = Arrays.asList(testElection);
        when(electionRepository.findAll()).thenReturn(mockElections);
        
        // Act
        List<Election> result = adminService.getAllElections(null);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(electionRepository).findAll();
    }
    
    @Test
    void getAllElections_WithStatusFilter_ShouldReturnFilteredElections() {
        // Arrange
        List<Election> mockElections = Arrays.asList(testElection);
        when(electionRepository.findByStatusOrderByStartTimeAsc(ElectionStatus.DRAFT))
            .thenReturn(mockElections);
        
        // Act
        List<Election> result = adminService.getAllElections("DRAFT");
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(electionRepository).findByStatusOrderByStartTimeAsc(ElectionStatus.DRAFT);
    }
    
    @Test
    void activateElection_ValidElection_ShouldActivateElection() {
        // Arrange
        when(electionRepository.findById("election-123")).thenReturn(Optional.of(testElection));
        when(candidateRepository.countByElectionIdAndEnabledTrue("election-123")).thenReturn(2L);
        when(electionRepository.save(any(Election.class))).thenReturn(testElection);
        
        // Act
        Election result = adminService.activateElection("election-123", "admin-123");
        
        // Assert
        assertNotNull(result);
        verify(electionRepository).save(testElection);
        verify(auditService).logAdminAction(eq("admin-123"), eq("ACTIVATE_ELECTION"), anyString(), anyString());
    }
    
    @Test
    void activateElection_InsufficientCandidates_ShouldThrowException() {
        // Arrange
        when(electionRepository.findById("election-123")).thenReturn(Optional.of(testElection));
        when(candidateRepository.countByElectionIdAndEnabledTrue("election-123")).thenReturn(1L);
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> adminService.activateElection("election-123", "admin-123"));
    }
    
    @Test
    void completeElection_ValidElection_ShouldCompleteElection() {
        // Arrange
        testElection.setStatus(ElectionStatus.ACTIVE);
        when(electionRepository.findById("election-123")).thenReturn(Optional.of(testElection));
        when(electionRepository.save(any(Election.class))).thenReturn(testElection);
        
        // Act
        Election result = adminService.completeElection("election-123", "admin-123");
        
        // Assert
        assertNotNull(result);
        verify(electionRepository).save(testElection);
        verify(auditService).logAdminAction(eq("admin-123"), eq("COMPLETE_ELECTION"), anyString(), anyString());
    }
    
    @Test
    void completeElection_NotActiveElection_ShouldThrowException() {
        // Arrange
        when(electionRepository.findById("election-123")).thenReturn(Optional.of(testElection));
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> adminService.completeElection("election-123", "admin-123"));
    }
    
    @Test
    void validateElectionIntegrity_ValidElection_ShouldReturnNoIssues() {
        // Arrange
        when(electionRepository.findById("election-123")).thenReturn(Optional.of(testElection));
        when(voteRepository.findByElectionIdOrderByTimestampAsc("election-123"))
            .thenReturn(Arrays.asList(testVote));
        when(candidateRepository.findByElectionIdOrderByDisplayOrderAsc("election-123"))
            .thenReturn(testCandidates);
        when(cryptographyService.decryptVote(any(EncryptedVote.class)))
            .thenReturn("candidate-1");
        when(cryptographyService.verifyHash("candidate-1", "vote-hash")).thenReturn(true);
        
        // Act
        List<String> result = adminService.validateElectionIntegrity("election-123");
        
        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void validateElectionIntegrity_CorruptedVotes_ShouldReturnIssues() {
        // Arrange
        when(electionRepository.findById("election-123")).thenReturn(Optional.of(testElection));
        when(voteRepository.findByElectionIdOrderByTimestampAsc("election-123"))
            .thenReturn(Arrays.asList(testVote));
        when(candidateRepository.findByElectionIdOrderByDisplayOrderAsc("election-123"))
            .thenReturn(testCandidates);
        when(cryptographyService.decryptVote(any(EncryptedVote.class)))
            .thenThrow(new RuntimeException("Decryption failed"));
        
        // Act
        List<String> result = adminService.validateElectionIntegrity("election-123");
        
        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.get(0).contains("validation failed"));
    }
    
    private AuditLog createMockAuditLog(String logId, String userId, String action, AuditEventType eventType, String ipAddress) {
        AuditLog log = new AuditLog(logId, userId, action, eventType);
        log.setIpAddress(ipAddress);
        log.setDetails("Test audit log");
        return log;
    }
}