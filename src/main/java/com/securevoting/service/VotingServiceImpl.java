package com.securevoting.service;

import com.securevoting.exception.VotingException;
import com.securevoting.model.Election;
import com.securevoting.model.EncryptedVote;
import com.securevoting.model.Vote;
import com.securevoting.repository.CandidateRepository;
import com.securevoting.repository.ElectionRepository;
import com.securevoting.repository.VoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of VotingService for secure vote casting and election management.
 * Handles vote encryption, duplicate prevention, and election validation.
 */
@Service
@Transactional
public class VotingServiceImpl implements VotingService {
    
    private static final Logger logger = LoggerFactory.getLogger(VotingServiceImpl.class);
    
    private final VoteRepository voteRepository;
    private final ElectionRepository electionRepository;
    private final CandidateRepository candidateRepository;
    private final CryptographyService cryptographyService;
    
    @Autowired
    public VotingServiceImpl(VoteRepository voteRepository,
                           ElectionRepository electionRepository,
                           CandidateRepository candidateRepository,
                           CryptographyService cryptographyService) {
        this.voteRepository = voteRepository;
        this.electionRepository = electionRepository;
        this.candidateRepository = candidateRepository;
        this.cryptographyService = cryptographyService;
    }
    
    @Override
    public VoteResult castVote(String userId, String candidateId, String electionId, String ipAddress, String userAgent) {
        logger.info("Attempting to cast vote - userId: {}, candidateId: {}, electionId: {}", userId, candidateId, electionId);
        
        try {
            // Validate input parameters
            if (userId == null || userId.trim().isEmpty()) {
                return VoteResult.failure("User ID is required", "INVALID_USER");
            }
            if (candidateId == null || candidateId.trim().isEmpty()) {
                return VoteResult.failure("Candidate ID is required", "INVALID_CANDIDATE");
            }
            if (electionId == null || electionId.trim().isEmpty()) {
                return VoteResult.failure("Election ID is required", "INVALID_ELECTION");
            }
            
            // Check if user has already voted in this election
            if (hasUserVoted(userId, electionId)) {
                logger.warn("Duplicate vote attempt - userId: {}, electionId: {}", userId, electionId);
                return VoteResult.failure("You have already voted in this election", "DUPLICATE_VOTE");
            }
            
            // Validate election is active
            if (!isElectionActive(electionId)) {
                logger.warn("Vote attempt on inactive election - electionId: {}", electionId);
                return VoteResult.failure("Election is not currently active", "ELECTION_INACTIVE");
            }
            
            // Validate candidate exists in election
            if (!isCandidateValid(candidateId, electionId)) {
                logger.warn("Invalid candidate for election - candidateId: {}, electionId: {}", candidateId, electionId);
                return VoteResult.failure("Invalid candidate for this election", "INVALID_CANDIDATE");
            }
            
            // Create vote data for encryption
            String voteData = createVoteData(userId, candidateId, electionId);
            
            // Encrypt the vote
            EncryptedVote encryptedVote = cryptographyService.encryptVote(voteData, electionId);
            
            // Create and save the vote entity
            Vote vote = new Vote();
            vote.setVoteId(UUID.randomUUID().toString());
            vote.setUserId(userId);
            vote.setElectionId(electionId);
            vote.setEncryptedVoteData(encryptedVote.getEncryptedData());
            vote.setVoteHash(encryptedVote.getHash());
            vote.setInitializationVector(encryptedVote.getInitializationVector());
            vote.setTimestamp(LocalDateTime.now());
            vote.setIpAddress(ipAddress);
            vote.setUserAgent(userAgent);
            
            // Save the vote
            Vote savedVote = voteRepository.save(vote);
            
            logger.info("Vote cast successfully - voteId: {}, userId: {}, electionId: {}", savedVote.getVoteId(), userId, electionId);
            return VoteResult.success(savedVote.getVoteId(), "Your vote has been cast successfully");
            
        } catch (Exception e) {
            logger.error("Error casting vote - userId: {}, candidateId: {}, electionId: {}", userId, candidateId, electionId, e);
            return VoteResult.failure("Failed to cast vote: " + e.getMessage(), "VOTE_CAST_ERROR");
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean hasUserVoted(String userId, String electionId) {
        if (userId == null || electionId == null) {
            return false;
        }
        return voteRepository.existsByUserIdAndElectionId(userId, electionId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Election> getActiveElections() {
        LocalDateTime now = LocalDateTime.now();
        return electionRepository.findActiveElections(now);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Election getElectionDetails(String electionId) {
        if (electionId == null || electionId.trim().isEmpty()) {
            return null;
        }
        
        return electionRepository.findById(electionId).orElse(null);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isElectionActive(String electionId) {
        if (electionId == null || electionId.trim().isEmpty()) {
            return false;
        }
        
        Election election = electionRepository.findById(electionId).orElse(null);
        if (election == null) {
            return false;
        }
        
        return election.isActive();
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isCandidateValid(String candidateId, String electionId) {
        if (candidateId == null || electionId == null) {
            return false;
        }
        
        return candidateRepository.findById(candidateId)
                .map(candidate -> candidate.getElectionId().equals(electionId) && candidate.isEnabled())
                .orElse(false);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Vote getUserVote(String userId, String electionId) {
        if (userId == null || electionId == null) {
            return null;
        }
        
        return voteRepository.findByUserIdAndElectionId(userId, electionId).orElse(null);
    }
    
    @Override
    @Transactional(readOnly = true)
    public long getVoteCount(String electionId) {
        if (electionId == null || electionId.trim().isEmpty()) {
            return 0;
        }
        
        return voteRepository.countByElectionId(electionId);
    }
    
    /**
     * Creates the vote data string for encryption.
     * Format: "userId:candidateId:electionId:timestamp"
     */
    private String createVoteData(String userId, String candidateId, String electionId) {
        return String.format("%s:%s:%s:%s", userId, candidateId, electionId, LocalDateTime.now().toString());
    }
}