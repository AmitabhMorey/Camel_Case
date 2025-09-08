package com.securevoting.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * Vote entity representing encrypted votes in the system.
 */
@Entity
@Table(name = "votes", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "election_id"}))
public class Vote {
    
    @Id
    @Column(name = "vote_id", length = 36)
    private String voteId;
    
    @NotBlank(message = "User ID is required")
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;
    
    @NotBlank(message = "Election ID is required")
    @Column(name = "election_id", nullable = false, length = 36)
    private String electionId;
    
    @NotBlank(message = "Encrypted vote data is required")
    @Column(name = "encrypted_vote_data", nullable = false, columnDefinition = "TEXT")
    private String encryptedVoteData;
    
    @NotBlank(message = "Vote hash is required")
    @Column(name = "vote_hash", nullable = false, length = 255)
    private String voteHash;
    
    @Column(name = "initialization_vector", length = 255)
    private String initializationVector;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    // JPA relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "election_id", insertable = false, updatable = false)
    private Election election;
    
    // Constructors
    public Vote() {
        this.timestamp = LocalDateTime.now();
    }
    
    public Vote(String voteId, String userId, String electionId, 
               String encryptedVoteData, String voteHash, String initializationVector) {
        this();
        this.voteId = voteId;
        this.userId = userId;
        this.electionId = electionId;
        this.encryptedVoteData = encryptedVoteData;
        this.voteHash = voteHash;
        this.initializationVector = initializationVector;
    }
    
    // Getters and Setters
    public String getVoteId() {
        return voteId;
    }
    
    public void setVoteId(String voteId) {
        this.voteId = voteId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getElectionId() {
        return electionId;
    }
    
    public void setElectionId(String electionId) {
        this.electionId = electionId;
    }
    
    public String getEncryptedVoteData() {
        return encryptedVoteData;
    }
    
    public void setEncryptedVoteData(String encryptedVoteData) {
        this.encryptedVoteData = encryptedVoteData;
    }
    
    public String getVoteHash() {
        return voteHash;
    }
    
    public void setVoteHash(String voteHash) {
        this.voteHash = voteHash;
    }
    
    public String getInitializationVector() {
        return initializationVector;
    }
    
    public void setInitializationVector(String initializationVector) {
        this.initializationVector = initializationVector;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public Election getElection() {
        return election;
    }
    
    public void setElection(Election election) {
        this.election = election;
    }
    
    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
    
    @Override
    public String toString() {
        return "Vote{" +
                "voteId='" + voteId + '\'' +
                ", userId='" + userId + '\'' +
                ", electionId='" + electionId + '\'' +
                ", timestamp=" + timestamp +
                ", ipAddress='" + ipAddress + '\'' +
                '}';
    }
}