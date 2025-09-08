package com.securevoting.service;

import com.securevoting.model.Election;
import com.securevoting.model.Vote;

import java.util.List;

/**
 * Service interface for voting operations including vote casting, validation, and election management.
 * Handles secure vote storage with encryption and duplicate vote prevention.
 */
public interface VotingService {
    
    /**
     * Casts a vote for a candidate in an election.
     * Encrypts the vote data and stores it securely with integrity verification.
     * 
     * @param userId the ID of the user casting the vote
     * @param candidateId the ID of the candidate being voted for
     * @param electionId the ID of the election
     * @param ipAddress the IP address of the voter (for audit purposes)
     * @param userAgent the user agent string (for audit purposes)
     * @return VoteResult containing success status and details
     * @throws VotingException if vote casting fails
     */
    VoteResult castVote(String userId, String candidateId, String electionId, String ipAddress, String userAgent);
    
    /**
     * Checks if a user has already voted in a specific election.
     * 
     * @param userId the ID of the user to check
     * @param electionId the ID of the election to check
     * @return true if the user has already voted, false otherwise
     */
    boolean hasUserVoted(String userId, String electionId);
    
    /**
     * Retrieves all currently active elections that users can vote in.
     * 
     * @return list of active elections
     */
    List<Election> getActiveElections();
    
    /**
     * Retrieves detailed information about a specific election including candidates.
     * 
     * @param electionId the ID of the election
     * @return Election details with candidates, or null if not found
     */
    Election getElectionDetails(String electionId);
    
    /**
     * Validates that an election is available for voting.
     * 
     * @param electionId the ID of the election to validate
     * @return true if the election is active and accepting votes, false otherwise
     */
    boolean isElectionActive(String electionId);
    
    /**
     * Validates that a candidate exists in the specified election.
     * 
     * @param candidateId the ID of the candidate to validate
     * @param electionId the ID of the election
     * @return true if the candidate exists in the election, false otherwise
     */
    boolean isCandidateValid(String candidateId, String electionId);
    
    /**
     * Retrieves the vote cast by a specific user in an election (for verification purposes).
     * Note: This returns encrypted vote data for integrity verification only.
     * 
     * @param userId the ID of the user
     * @param electionId the ID of the election
     * @return Vote entity if found, null otherwise
     */
    Vote getUserVote(String userId, String electionId);
    
    /**
     * Gets the total number of votes cast in an election.
     * 
     * @param electionId the ID of the election
     * @return the total vote count
     */
    long getVoteCount(String electionId);
}