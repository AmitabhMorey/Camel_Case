package com.securevoting.service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * System statistics for admin dashboard display.
 * Contains comprehensive metrics about the voting system.
 */
public class SystemStatistics {
    
    private final int totalUsers;
    private final int totalElections;
    private final int activeElections;
    private final int completedElections;
    private final int totalVotes;
    private final int votesToday;
    private final int authenticationAttempts;
    private final int failedAuthentications;
    private final Map<String, Integer> electionStatusCounts;
    private final Map<String, Integer> recentActivityCounts;
    private final LocalDateTime generatedAt;
    
    public SystemStatistics(int totalUsers, int totalElections, int activeElections, 
                           int completedElections, int totalVotes, int votesToday,
                           int authenticationAttempts, int failedAuthentications,
                           Map<String, Integer> electionStatusCounts,
                           Map<String, Integer> recentActivityCounts) {
        this.totalUsers = totalUsers;
        this.totalElections = totalElections;
        this.activeElections = activeElections;
        this.completedElections = completedElections;
        this.totalVotes = totalVotes;
        this.votesToday = votesToday;
        this.authenticationAttempts = authenticationAttempts;
        this.failedAuthentications = failedAuthentications;
        this.electionStatusCounts = electionStatusCounts;
        this.recentActivityCounts = recentActivityCounts;
        this.generatedAt = LocalDateTime.now();
    }
    
    // Getters
    public int getTotalUsers() {
        return totalUsers;
    }
    
    public int getTotalElections() {
        return totalElections;
    }
    
    public int getActiveElections() {
        return activeElections;
    }
    
    public int getCompletedElections() {
        return completedElections;
    }
    
    public int getTotalVotes() {
        return totalVotes;
    }
    
    public int getVotesToday() {
        return votesToday;
    }
    
    public int getAuthenticationAttempts() {
        return authenticationAttempts;
    }
    
    public int getFailedAuthentications() {
        return failedAuthentications;
    }
    
    public Map<String, Integer> getElectionStatusCounts() {
        return electionStatusCounts;
    }
    
    public Map<String, Integer> getRecentActivityCounts() {
        return recentActivityCounts;
    }
    
    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }
    
    // Calculated metrics
    public double getAuthenticationSuccessRate() {
        if (authenticationAttempts == 0) {
            return 0.0;
        }
        return (double) (authenticationAttempts - failedAuthentications) / authenticationAttempts * 100.0;
    }
    
    public double getAverageVotesPerElection() {
        if (totalElections == 0) {
            return 0.0;
        }
        return (double) totalVotes / totalElections;
    }
    
    public double getVoterParticipationRate() {
        if (totalUsers == 0) {
            return 0.0;
        }
        return (double) totalVotes / totalUsers * 100.0;
    }
    
    @Override
    public String toString() {
        return "SystemStatistics{" +
                "totalUsers=" + totalUsers +
                ", totalElections=" + totalElections +
                ", activeElections=" + activeElections +
                ", completedElections=" + completedElections +
                ", totalVotes=" + totalVotes +
                ", votesToday=" + votesToday +
                ", authenticationAttempts=" + authenticationAttempts +
                ", failedAuthentications=" + failedAuthentications +
                ", generatedAt=" + generatedAt +
                '}';
    }
}