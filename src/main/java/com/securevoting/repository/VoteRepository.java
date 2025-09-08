package com.securevoting.repository;

import com.securevoting.model.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Vote entity operations.
 */
@Repository
public interface VoteRepository extends JpaRepository<Vote, String> {
    
    /**
     * Find vote by user and election (should be unique due to constraint).
     */
    Optional<Vote> findByUserIdAndElectionId(String userId, String electionId);
    
    /**
     * Check if user has already voted in an election.
     */
    boolean existsByUserIdAndElectionId(String userId, String electionId);
    
    /**
     * Find all votes for a specific election.
     */
    List<Vote> findByElectionIdOrderByTimestampAsc(String electionId);
    
    /**
     * Find all votes by a specific user.
     */
    List<Vote> findByUserIdOrderByTimestampDesc(String userId);
    
    /**
     * Count votes for a specific election.
     */
    long countByElectionId(String electionId);
    
    /**
     * Count votes by a specific user.
     */
    long countByUserId(String userId);
    
    /**
     * Find votes within a time range.
     */
    @Query("SELECT v FROM Vote v WHERE v.timestamp BETWEEN :startTime AND :endTime ORDER BY v.timestamp ASC")
    List<Vote> findVotesInTimeRange(@Param("startTime") LocalDateTime startTime, 
                                   @Param("endTime") LocalDateTime endTime);
    
    /**
     * Find votes for an election within a time range.
     */
    @Query("SELECT v FROM Vote v WHERE v.electionId = :electionId AND v.timestamp BETWEEN :startTime AND :endTime ORDER BY v.timestamp ASC")
    List<Vote> findVotesForElectionInTimeRange(@Param("electionId") String electionId,
                                              @Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime);
    
    /**
     * Find votes by IP address (for security monitoring).
     */
    List<Vote> findByIpAddressOrderByTimestampDesc(String ipAddress);
    
    /**
     * Count votes from a specific IP address.
     */
    long countByIpAddress(String ipAddress);
    
    /**
     * Find recent votes (for monitoring).
     */
    @Query("SELECT v FROM Vote v WHERE v.timestamp >= :since ORDER BY v.timestamp DESC")
    List<Vote> findRecentVotes(@Param("since") LocalDateTime since);
    
    /**
     * Get vote statistics for an election.
     */
    @Query("SELECT COUNT(v), MIN(v.timestamp), MAX(v.timestamp) FROM Vote v WHERE v.electionId = :electionId")
    Object[] getVoteStatisticsForElection(@Param("electionId") String electionId);
    
    /**
     * Find votes with potential integrity issues (for audit).
     */
    @Query("SELECT v FROM Vote v WHERE v.voteHash IS NULL OR v.encryptedVoteData IS NULL")
    List<Vote> findVotesWithIntegrityIssues();
}