package com.securevoting.repository;

import com.securevoting.model.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Candidate entity operations.
 */
@Repository
public interface CandidateRepository extends JpaRepository<Candidate, String> {
    
    /**
     * Find candidates by election ID ordered by display order.
     */
    List<Candidate> findByElectionIdOrderByDisplayOrderAsc(String electionId);
    
    /**
     * Find enabled candidates by election ID ordered by display order.
     */
    List<Candidate> findByElectionIdAndEnabledTrueOrderByDisplayOrderAsc(String electionId);
    
    /**
     * Find candidates by election ID ordered by name.
     */
    List<Candidate> findByElectionIdOrderByNameAsc(String electionId);
    
    /**
     * Count candidates for a specific election.
     */
    long countByElectionId(String electionId);
    
    /**
     * Count enabled candidates for a specific election.
     */
    long countByElectionIdAndEnabledTrue(String electionId);
    
    /**
     * Find candidates by name containing keyword.
     */
    @Query("SELECT c FROM Candidate c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY c.name ASC")
    List<Candidate> searchByName(@Param("keyword") String keyword);
    
    /**
     * Find candidates by election and name containing keyword.
     */
    @Query("SELECT c FROM Candidate c WHERE c.electionId = :electionId AND LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY c.displayOrder ASC")
    List<Candidate> searchByElectionAndName(@Param("electionId") String electionId, 
                                          @Param("keyword") String keyword);
    
    /**
     * Find the maximum display order for an election.
     */
    @Query("SELECT MAX(c.displayOrder) FROM Candidate c WHERE c.electionId = :electionId")
    Integer findMaxDisplayOrderForElection(@Param("electionId") String electionId);
    
    /**
     * Check if candidate name exists in election.
     */
    boolean existsByElectionIdAndName(String electionId, String name);
    
    /**
     * Find all enabled candidates.
     */
    List<Candidate> findByEnabledTrueOrderByCreatedAtDesc();
    
    /**
     * Find candidates with no display order set.
     */
    @Query("SELECT c FROM Candidate c WHERE c.displayOrder IS NULL ORDER BY c.createdAt ASC")
    List<Candidate> findCandidatesWithoutDisplayOrder();
}