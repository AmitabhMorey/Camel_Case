package com.securevoting.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Result object for election tallying operations.
 * Contains vote counts, percentages, and integrity information.
 */
public class ElectionResult {
    
    private final String electionId;
    private final String electionTitle;
    private final int totalVotes;
    private final int validVotes;
    private final int invalidVotes;
    private final Map<String, CandidateResult> candidateResults;
    private final LocalDateTime tallyTimestamp;
    private final List<String> integrityIssues;
    private final boolean tallySuccessful;
    
    public ElectionResult(String electionId, String electionTitle, int totalVotes, 
                         int validVotes, int invalidVotes, Map<String, CandidateResult> candidateResults,
                         List<String> integrityIssues, boolean tallySuccessful) {
        this.electionId = electionId;
        this.electionTitle = electionTitle;
        this.totalVotes = totalVotes;
        this.validVotes = validVotes;
        this.invalidVotes = invalidVotes;
        this.candidateResults = candidateResults;
        this.integrityIssues = integrityIssues;
        this.tallySuccessful = tallySuccessful;
        this.tallyTimestamp = LocalDateTime.now();
    }
    
    // Getters
    public String getElectionId() {
        return electionId;
    }
    
    public String getElectionTitle() {
        return electionTitle;
    }
    
    public int getTotalVotes() {
        return totalVotes;
    }
    
    public int getValidVotes() {
        return validVotes;
    }
    
    public int getInvalidVotes() {
        return invalidVotes;
    }
    
    public Map<String, CandidateResult> getCandidateResults() {
        return candidateResults;
    }
    
    public LocalDateTime getTallyTimestamp() {
        return tallyTimestamp;
    }
    
    public List<String> getIntegrityIssues() {
        return integrityIssues;
    }
    
    public boolean isTallySuccessful() {
        return tallySuccessful;
    }
    
    public double getTurnoutPercentage(int totalEligibleVoters) {
        if (totalEligibleVoters <= 0) {
            return 0.0;
        }
        return (double) totalVotes / totalEligibleVoters * 100.0;
    }
    
    /**
     * Inner class representing results for a specific candidate.
     */
    public static class CandidateResult {
        private final String candidateId;
        private final String candidateName;
        private final int voteCount;
        private final double percentage;
        
        public CandidateResult(String candidateId, String candidateName, int voteCount, double percentage) {
            this.candidateId = candidateId;
            this.candidateName = candidateName;
            this.voteCount = voteCount;
            this.percentage = percentage;
        }
        
        public String getCandidateId() {
            return candidateId;
        }
        
        public String getCandidateName() {
            return candidateName;
        }
        
        public int getVoteCount() {
            return voteCount;
        }
        
        public double getPercentage() {
            return percentage;
        }
        
        @Override
        public String toString() {
            return "CandidateResult{" +
                    "candidateId='" + candidateId + '\'' +
                    ", candidateName='" + candidateName + '\'' +
                    ", voteCount=" + voteCount +
                    ", percentage=" + percentage +
                    '}';
        }
    }
    
    @Override
    public String toString() {
        return "ElectionResult{" +
                "electionId='" + electionId + '\'' +
                ", electionTitle='" + electionTitle + '\'' +
                ", totalVotes=" + totalVotes +
                ", validVotes=" + validVotes +
                ", invalidVotes=" + invalidVotes +
                ", tallyTimestamp=" + tallyTimestamp +
                ", tallySuccessful=" + tallySuccessful +
                '}';
    }
}