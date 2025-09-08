package com.securevoting.service;

import com.securevoting.model.*;
import com.securevoting.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of AdminService for election management and administrative operations.
 */
@Service
@Transactional
public class AdminServiceImpl implements AdminService {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminServiceImpl.class);
    
    private final ElectionRepository electionRepository;
    private final CandidateRepository candidateRepository;
    private final VoteRepository voteRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final CryptographyService cryptographyService;
    private final AuditService auditService;
    
    @Autowired
    public AdminServiceImpl(ElectionRepository electionRepository,
                           CandidateRepository candidateRepository,
                           VoteRepository voteRepository,
                           UserRepository userRepository,
                           AuditLogRepository auditLogRepository,
                           CryptographyService cryptographyService,
                           AuditService auditService) {
        this.electionRepository = electionRepository;
        this.candidateRepository = candidateRepository;
        this.voteRepository = voteRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.cryptographyService = cryptographyService;
        this.auditService = auditService;
    }
    
    @Override
    public Election createElection(ElectionRequest request, String createdBy) {
        logger.info("Creating new election: {} by user: {}", request.getTitle(), createdBy);
        
        // Validate request
        if (!request.isValid()) {
            throw new IllegalArgumentException("Invalid election request");
        }
        
        if (request.getStartTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Election start time cannot be in the past");
        }
        
        if (request.getStartTime().isAfter(request.getEndTime())) {
            throw new IllegalArgumentException("Election start time must be before end time");
        }
        
        // Create election
        String electionId = UUID.randomUUID().toString();
        Election election = new Election(
            electionId,
            request.getTitle(),
            request.getDescription(),
            request.getStartTime(),
            request.getEndTime(),
            createdBy
        );
        
        election = electionRepository.save(election);
        
        // Create candidates
        List<Candidate> candidates = new ArrayList<>();
        for (int i = 0; i < request.getCandidates().size(); i++) {
            ElectionRequest.CandidateRequest candidateRequest = request.getCandidates().get(i);
            String candidateId = UUID.randomUUID().toString();
            
            Candidate candidate = new Candidate(
                candidateId,
                candidateRequest.getName(),
                candidateRequest.getDescription(),
                electionId
            );
            candidate.setDisplayOrder(candidateRequest.getDisplayOrder() != null ? 
                candidateRequest.getDisplayOrder() : i + 1);
            
            candidates.add(candidate);
        }
        
        candidateRepository.saveAll(candidates);
        election.setCandidates(candidates);
        
        // Log the creation
        auditService.logAdminAction(createdBy, "CREATE_ELECTION", 
            "Created election: " + election.getTitle() + " (ID: " + electionId + ")", "127.0.0.1");
        
        logger.info("Successfully created election: {} with {} candidates", 
            election.getTitle(), candidates.size());
        
        return election;
    }
    
    @Override
    public Election updateElection(String electionId, ElectionRequest request, String updatedBy) {
        logger.info("Updating election: {} by user: {}", electionId, updatedBy);
        
        Election election = electionRepository.findById(electionId)
            .orElseThrow(() -> new IllegalArgumentException("Election not found: " + electionId));
        
        // Check if election can be updated
        if (election.getStatus() == ElectionStatus.ACTIVE && 
            LocalDateTime.now().isAfter(election.getStartTime())) {
            throw new RuntimeException("Cannot update election that has already started");
        }
        
        if (election.getStatus() == ElectionStatus.COMPLETED) {
            throw new RuntimeException("Cannot update completed election");
        }
        
        // Validate request
        if (!request.isValid()) {
            throw new IllegalArgumentException("Invalid election request");
        }
        
        // Update election details
        election.setTitle(request.getTitle());
        election.setDescription(request.getDescription());
        election.setStartTime(request.getStartTime());
        election.setEndTime(request.getEndTime());
        
        election = electionRepository.save(election);
        
        // Update candidates if election hasn't started
        if (election.getStatus() == ElectionStatus.DRAFT) {
            // Remove existing candidates
            candidateRepository.deleteAll(election.getCandidates());
            
            // Create new candidates
            List<Candidate> newCandidates = new ArrayList<>();
            for (int i = 0; i < request.getCandidates().size(); i++) {
                ElectionRequest.CandidateRequest candidateRequest = request.getCandidates().get(i);
                String candidateId = UUID.randomUUID().toString();
                
                Candidate candidate = new Candidate(
                    candidateId,
                    candidateRequest.getName(),
                    candidateRequest.getDescription(),
                    electionId
                );
                candidate.setDisplayOrder(candidateRequest.getDisplayOrder() != null ? 
                    candidateRequest.getDisplayOrder() : i + 1);
                
                newCandidates.add(candidate);
            }
            
            candidateRepository.saveAll(newCandidates);
            election.setCandidates(newCandidates);
        }
        
        // Log the update
        auditService.logAdminAction(updatedBy, "UPDATE_ELECTION", 
            "Updated election: " + election.getTitle() + " (ID: " + electionId + ")", "127.0.0.1");
        
        logger.info("Successfully updated election: {}", election.getTitle());
        
        return election;
    }
    
    @Override
    public void deleteElection(String electionId, String deletedBy) {
        logger.info("Deleting election: {} by user: {}", electionId, deletedBy);
        
        Election election = electionRepository.findById(electionId)
            .orElseThrow(() -> new IllegalArgumentException("Election not found: " + electionId));
        
        // Check if election can be deleted
        if (election.getStatus() == ElectionStatus.ACTIVE || 
            election.getStatus() == ElectionStatus.COMPLETED) {
            throw new RuntimeException("Cannot delete active or completed election");
        }
        
        // Check if there are any votes
        long voteCount = voteRepository.countByElectionId(electionId);
        if (voteCount > 0) {
            throw new RuntimeException("Cannot delete election with existing votes");
        }
        
        String electionTitle = election.getTitle();
        
        // Delete candidates first (due to foreign key constraints)
        candidateRepository.deleteAll(election.getCandidates());
        
        // Delete election
        electionRepository.delete(election);
        
        // Log the deletion
        auditService.logAdminAction(deletedBy, "DELETE_ELECTION", 
            "Deleted election: " + electionTitle + " (ID: " + electionId + ")", "127.0.0.1");
        
        logger.info("Successfully deleted election: {}", electionTitle);
    }
    
    @Override
    @Transactional(readOnly = true)
    public ElectionResult tallyVotes(String electionId) {
        logger.info("Tallying votes for election: {}", electionId);
        
        Election election = electionRepository.findById(electionId)
            .orElseThrow(() -> new IllegalArgumentException("Election not found: " + electionId));
        
        // Get all votes for the election
        List<Vote> votes = voteRepository.findByElectionIdOrderByTimestampAsc(electionId);
        List<Candidate> candidates = candidateRepository.findByElectionIdOrderByDisplayOrderAsc(electionId);
        
        // Initialize vote counts
        Map<String, Integer> voteCounts = new HashMap<>();
        Map<String, String> candidateNames = new HashMap<>();
        
        for (Candidate candidate : candidates) {
            voteCounts.put(candidate.getCandidateId(), 0);
            candidateNames.put(candidate.getCandidateId(), candidate.getName());
        }
        
        int validVotes = 0;
        int invalidVotes = 0;
        List<String> integrityIssues = new ArrayList<>();
        
        // Process each vote
        for (Vote vote : votes) {
            try {
                // Create EncryptedVote object for decryption
                EncryptedVote encryptedVote = new EncryptedVote(
                    vote.getEncryptedVoteData(),
                    vote.getInitializationVector(),
                    vote.getVoteHash(),
                    "AES-256-GCM"
                );
                
                // Decrypt the vote
                String decryptedVoteData = cryptographyService.decryptVote(encryptedVote);
                
                // Verify hash integrity
                String expectedHash = cryptographyService.generateHash(decryptedVoteData);
                if (!cryptographyService.verifyHash(decryptedVoteData, vote.getVoteHash())) {
                    integrityIssues.add("Vote " + vote.getVoteId() + " failed hash verification");
                    invalidVotes++;
                    continue;
                }
                
                // Parse vote data (assuming format: "candidateId")
                String candidateId = decryptedVoteData.trim();
                
                // Validate candidate exists
                if (voteCounts.containsKey(candidateId)) {
                    voteCounts.put(candidateId, voteCounts.get(candidateId) + 1);
                    validVotes++;
                } else {
                    integrityIssues.add("Vote " + vote.getVoteId() + " contains invalid candidate ID: " + candidateId);
                    invalidVotes++;
                }
                
            } catch (Exception e) {
                logger.error("Error processing vote {}: {}", vote.getVoteId(), e.getMessage());
                integrityIssues.add("Vote " + vote.getVoteId() + " decryption failed: " + e.getMessage());
                invalidVotes++;
            }
        }
        
        // Calculate percentages and create results
        Map<String, ElectionResult.CandidateResult> candidateResults = new HashMap<>();
        
        for (Map.Entry<String, Integer> entry : voteCounts.entrySet()) {
            String candidateId = entry.getKey();
            int voteCount = entry.getValue();
            double percentage = validVotes > 0 ? (double) voteCount / validVotes * 100.0 : 0.0;
            
            candidateResults.put(candidateId, new ElectionResult.CandidateResult(
                candidateId,
                candidateNames.get(candidateId),
                voteCount,
                percentage
            ));
        }
        
        boolean tallySuccessful = integrityIssues.isEmpty();
        int totalVotes = validVotes + invalidVotes;
        
        ElectionResult result = new ElectionResult(
            electionId,
            election.getTitle(),
            totalVotes,
            validVotes,
            invalidVotes,
            candidateResults,
            integrityIssues,
            tallySuccessful
        );
        
        // Log the tallying
        auditService.logAdminAction("SYSTEM", "TALLY_VOTES", 
            String.format("Tallied votes for election %s: %d total, %d valid, %d invalid", 
                election.getTitle(), totalVotes, validVotes, invalidVotes), "127.0.0.1");
        
        logger.info("Successfully tallied votes for election: {} - {} total votes, {} valid, {} invalid", 
            election.getTitle(), totalVotes, validVotes, invalidVotes);
        
        return result;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogs(AuditFilter filter) {
        logger.debug("Retrieving audit logs with filter: {}", filter);
        
        if (filter == null || !filter.hasFilters()) {
            // Return recent logs with pagination
            Pageable pageable = PageRequest.of(0, 100);
            return auditLogRepository.findAllByOrderByTimestampDesc(pageable).getContent();
        }
        
        // Use the complex filter query
        return auditLogRepository.findByFilters(
            filter.getUserId(),
            filter.getEventType(),
            filter.getStartDate(),
            filter.getEndDate(),
            filter.getIpAddress()
        );
    }
    
    @Override
    @Transactional(readOnly = true)
    public SystemStatistics getSystemStatistics() {
        logger.debug("Gathering system statistics");
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        
        // Basic counts
        int totalUsers = (int) userRepository.count();
        int totalElections = (int) electionRepository.count();
        int activeElections = electionRepository.findActiveElections(now).size();
        int completedElections = electionRepository.findCompletedElections(now).size();
        int totalVotes = (int) voteRepository.count();
        
        // Today's votes
        List<Vote> todayVotes = voteRepository.findVotesInTimeRange(todayStart, now);
        int votesToday = todayVotes.size();
        
        // Authentication statistics
        LocalDateTime last24Hours = now.minusDays(1);
        List<AuditLog> authLogs = auditLogRepository.findLogsInTimeRange(last24Hours, now);
        
        int authenticationAttempts = 0;
        int failedAuthentications = 0;
        
        for (AuditLog log : authLogs) {
            if (log.getEventType() == AuditEventType.AUTH_SUCCESS || 
                log.getEventType() == AuditEventType.AUTH_FAILURE) {
                authenticationAttempts++;
                if (log.getEventType() == AuditEventType.AUTH_FAILURE) {
                    failedAuthentications++;
                }
            }
        }
        
        // Election status counts
        Map<String, Integer> electionStatusCounts = new HashMap<>();
        electionStatusCounts.put("DRAFT", (int) electionRepository.countByStatus(ElectionStatus.DRAFT));
        electionStatusCounts.put("ACTIVE", activeElections);
        electionStatusCounts.put("COMPLETED", completedElections);
        
        // Recent activity counts
        Map<String, Integer> recentActivityCounts = new HashMap<>();
        Map<AuditEventType, Long> eventTypeCounts = authLogs.stream()
            .collect(Collectors.groupingBy(AuditLog::getEventType, Collectors.counting()));
        
        for (Map.Entry<AuditEventType, Long> entry : eventTypeCounts.entrySet()) {
            recentActivityCounts.put(entry.getKey().name(), entry.getValue().intValue());
        }
        
        SystemStatistics statistics = new SystemStatistics(
            totalUsers,
            totalElections,
            activeElections,
            completedElections,
            totalVotes,
            votesToday,
            authenticationAttempts,
            failedAuthentications,
            electionStatusCounts,
            recentActivityCounts
        );
        
        logger.debug("Generated system statistics: {} users, {} elections, {} votes", 
            totalUsers, totalElections, totalVotes);
        
        return statistics;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Election> getAllElections(String statusFilter) {
        logger.debug("Retrieving all elections with status filter: {}", statusFilter);
        
        if (statusFilter == null || statusFilter.trim().isEmpty()) {
            return electionRepository.findAll();
        }
        
        try {
            ElectionStatus status = ElectionStatus.valueOf(statusFilter.toUpperCase());
            return electionRepository.findByStatusOrderByStartTimeAsc(status);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid election status filter: {}", statusFilter);
            return electionRepository.findAll();
        }
    }
    
    @Override
    public Election activateElection(String electionId, String activatedBy) {
        logger.info("Activating election: {} by user: {}", electionId, activatedBy);
        
        Election election = electionRepository.findById(electionId)
            .orElseThrow(() -> new IllegalArgumentException("Election not found: " + electionId));
        
        if (election.getStatus() != ElectionStatus.DRAFT) {
            throw new IllegalArgumentException("Only draft elections can be activated");
        }
        
        // Validate election has candidates
        long candidateCount = candidateRepository.countByElectionIdAndEnabledTrue(electionId);
        if (candidateCount < 2) {
            throw new IllegalArgumentException("Election must have at least 2 candidates to be activated");
        }
        
        election.setStatus(ElectionStatus.ACTIVE);
        election = electionRepository.save(election);
        
        // Log the activation
        auditService.logAdminAction(activatedBy, "ACTIVATE_ELECTION", 
            "Activated election: " + election.getTitle() + " (ID: " + electionId + ")", "127.0.0.1");
        
        logger.info("Successfully activated election: {}", election.getTitle());
        
        return election;
    }
    
    @Override
    public Election completeElection(String electionId, String completedBy) {
        logger.info("Completing election: {} by user: {}", electionId, completedBy);
        
        Election election = electionRepository.findById(electionId)
            .orElseThrow(() -> new IllegalArgumentException("Election not found: " + electionId));
        
        if (election.getStatus() != ElectionStatus.ACTIVE) {
            throw new IllegalArgumentException("Only active elections can be completed");
        }
        
        election.setStatus(ElectionStatus.COMPLETED);
        election = electionRepository.save(election);
        
        // Log the completion
        auditService.logAdminAction(completedBy, "COMPLETE_ELECTION", 
            "Completed election: " + election.getTitle() + " (ID: " + electionId + ")", "127.0.0.1");
        
        logger.info("Successfully completed election: {}", election.getTitle());
        
        return election;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<String> validateElectionIntegrity(String electionId) {
        logger.info("Validating integrity for election: {}", electionId);
        
        Election election = electionRepository.findById(electionId)
            .orElseThrow(() -> new IllegalArgumentException("Election not found: " + electionId));
        
        List<String> issues = new ArrayList<>();
        List<Vote> votes = voteRepository.findByElectionIdOrderByTimestampAsc(electionId);
        List<Candidate> candidates = candidateRepository.findByElectionIdOrderByDisplayOrderAsc(electionId);
        
        // Create candidate ID set for validation
        Set<String> validCandidateIds = candidates.stream()
            .map(Candidate::getCandidateId)
            .collect(Collectors.toSet());
        
        // Validate each vote
        for (Vote vote : votes) {
            try {
                // Check for missing data
                if (vote.getEncryptedVoteData() == null || vote.getEncryptedVoteData().trim().isEmpty()) {
                    issues.add("Vote " + vote.getVoteId() + " has missing encrypted data");
                    continue;
                }
                
                if (vote.getVoteHash() == null || vote.getVoteHash().trim().isEmpty()) {
                    issues.add("Vote " + vote.getVoteId() + " has missing hash");
                    continue;
                }
                
                // Try to decrypt and validate
                EncryptedVote encryptedVote = new EncryptedVote(
                    vote.getEncryptedVoteData(),
                    vote.getInitializationVector(),
                    vote.getVoteHash(),
                    "AES-256-GCM"
                );
                
                String decryptedVoteData = cryptographyService.decryptVote(encryptedVote);
                
                // Verify hash
                if (!cryptographyService.verifyHash(decryptedVoteData, vote.getVoteHash())) {
                    issues.add("Vote " + vote.getVoteId() + " failed hash verification");
                    continue;
                }
                
                // Validate candidate ID
                String candidateId = decryptedVoteData.trim();
                if (!validCandidateIds.contains(candidateId)) {
                    issues.add("Vote " + vote.getVoteId() + " contains invalid candidate ID: " + candidateId);
                }
                
            } catch (Exception e) {
                issues.add("Vote " + vote.getVoteId() + " validation failed: " + e.getMessage());
            }
        }
        
        logger.info("Integrity validation completed for election: {} - {} issues found", 
            election.getTitle(), issues.size());
        
        return issues;
    }
}