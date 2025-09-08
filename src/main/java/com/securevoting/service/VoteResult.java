package com.securevoting.service;

import java.time.LocalDateTime;

/**
 * Result object for vote casting operations.
 * Contains information about the success or failure of a vote attempt.
 */
public class VoteResult {
    
    private final boolean successful;
    private final String message;
    private final String voteId;
    private final LocalDateTime timestamp;
    private final String errorCode;
    
    // Private constructor to enforce use of factory methods
    private VoteResult(boolean successful, String message, String voteId, LocalDateTime timestamp, String errorCode) {
        this.successful = successful;
        this.message = message;
        this.voteId = voteId;
        this.timestamp = timestamp;
        this.errorCode = errorCode;
    }
    
    /**
     * Creates a successful vote result.
     * 
     * @param voteId the ID of the successfully cast vote
     * @param message success message
     * @return successful VoteResult
     */
    public static VoteResult success(String voteId, String message) {
        return new VoteResult(true, message, voteId, LocalDateTime.now(), null);
    }
    
    /**
     * Creates a successful vote result with default message.
     * 
     * @param voteId the ID of the successfully cast vote
     * @return successful VoteResult
     */
    public static VoteResult success(String voteId) {
        return success(voteId, "Vote cast successfully");
    }
    
    /**
     * Creates a failed vote result.
     * 
     * @param message error message
     * @param errorCode error code for categorization
     * @return failed VoteResult
     */
    public static VoteResult failure(String message, String errorCode) {
        return new VoteResult(false, message, null, LocalDateTime.now(), errorCode);
    }
    
    /**
     * Creates a failed vote result with default error code.
     * 
     * @param message error message
     * @return failed VoteResult
     */
    public static VoteResult failure(String message) {
        return failure(message, "VOTE_ERROR");
    }
    
    // Getters
    public boolean isSuccessful() {
        return successful;
    }
    
    public String getMessage() {
        return message;
    }
    
    public String getVoteId() {
        return voteId;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    @Override
    public String toString() {
        return "VoteResult{" +
                "successful=" + successful +
                ", message='" + message + '\'' +
                ", voteId='" + voteId + '\'' +
                ", timestamp=" + timestamp +
                ", errorCode='" + errorCode + '\'' +
                '}';
    }
}