package com.securevoting.service;

import com.securevoting.model.AuditLog;
import com.securevoting.model.Election;
import java.util.List;

/**
 * Service interface for administrative operations including election management,
 * vote tallying, and system statistics.
 */
public interface AdminService {
    
    /**
     * Creates a new election with the provided details and candidates.
     * 
     * @param request the election creation request containing all necessary data
     * @param createdBy the ID of the admin user creating the election
     * @return the created Election entity
     * @throws IllegalArgumentException if the request is invalid
     * @throws RuntimeException if election creation fails
     */
    Election createElection(ElectionRequest request, String createdBy);
    
    /**
     * Updates an existing election's details.
     * 
     * @param electionId the ID of the election to update
     * @param request the updated election data
     * @param updatedBy the ID of the admin user updating the election
     * @return the updated Election entity
     * @throws IllegalArgumentException if the election doesn't exist or request is invalid
     * @throws RuntimeException if the election cannot be updated (e.g., already started)
     */
    Election updateElection(String electionId, ElectionRequest request, String updatedBy);
    
    /**
     * Deletes an election if it hasn't started yet.
     * 
     * @param electionId the ID of the election to delete
     * @param deletedBy the ID of the admin user deleting the election
     * @throws IllegalArgumentException if the election doesn't exist
     * @throws RuntimeException if the election cannot be deleted (e.g., already started)
     */
    void deleteElection(String electionId, String deletedBy);
    
    /**
     * Tallies votes for a completed election, decrypting votes and calculating results.
     * 
     * @param electionId the ID of the election to tally
     * @return ElectionResult containing vote counts, percentages, and integrity information
     * @throws IllegalArgumentException if the election doesn't exist
     * @throws RuntimeException if tallying fails due to encryption or integrity issues
     */
    ElectionResult tallyVotes(String electionId);
    
    /**
     * Retrieves audit logs with optional filtering.
     * 
     * @param filter the audit filter criteria (can be null for all logs)
     * @return list of audit logs matching the filter criteria
     */
    List<AuditLog> getAuditLogs(AuditFilter filter);
    
    /**
     * Gathers comprehensive system statistics for dashboard display.
     * 
     * @return SystemStatistics containing various system metrics
     */
    SystemStatistics getSystemStatistics();
    
    /**
     * Retrieves all elections with optional status filtering.
     * 
     * @param statusFilter the election status to filter by (null for all elections)
     * @return list of elections matching the filter
     */
    List<Election> getAllElections(String statusFilter);
    
    /**
     * Activates an election, changing its status to ACTIVE.
     * 
     * @param electionId the ID of the election to activate
     * @param activatedBy the ID of the admin user activating the election
     * @return the updated Election entity
     * @throws IllegalArgumentException if the election doesn't exist or cannot be activated
     */
    Election activateElection(String electionId, String activatedBy);
    
    /**
     * Completes an election, changing its status to COMPLETED.
     * 
     * @param electionId the ID of the election to complete
     * @param completedBy the ID of the admin user completing the election
     * @return the updated Election entity
     * @throws IllegalArgumentException if the election doesn't exist or cannot be completed
     */
    Election completeElection(String electionId, String completedBy);
    
    /**
     * Validates the integrity of all votes in an election.
     * 
     * @param electionId the ID of the election to validate
     * @return list of integrity issues found (empty if no issues)
     */
    List<String> validateElectionIntegrity(String electionId);
}