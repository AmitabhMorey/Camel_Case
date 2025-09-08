package com.securevoting.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Request object for creating new elections.
 * Contains all necessary information to create an election with candidates.
 */
public class ElectionRequest {
    
    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
    private String title;
    
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;
    
    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;
    
    @NotNull(message = "End time is required")
    private LocalDateTime endTime;
    
    @NotNull(message = "At least one candidate is required")
    @Size(min = 1, message = "At least one candidate is required")
    private List<CandidateRequest> candidates;
    
    // Constructors
    public ElectionRequest() {}
    
    public ElectionRequest(String title, String description, LocalDateTime startTime, 
                          LocalDateTime endTime, List<CandidateRequest> candidates) {
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.candidates = candidates;
    }
    
    // Getters and Setters
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    
    public List<CandidateRequest> getCandidates() {
        return candidates;
    }
    
    public void setCandidates(List<CandidateRequest> candidates) {
        this.candidates = candidates;
    }
    
    /**
     * Validates the election request data.
     * 
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        if (title == null || title.trim().isEmpty()) {
            return false;
        }
        if (startTime == null || endTime == null) {
            return false;
        }
        if (startTime.isAfter(endTime)) {
            return false;
        }
        if (candidates == null || candidates.isEmpty()) {
            return false;
        }
        return candidates.stream().allMatch(CandidateRequest::isValid);
    }
    
    /**
     * Inner class representing a candidate in the election request.
     */
    public static class CandidateRequest {
        
        @NotBlank(message = "Candidate name is required")
        @Size(min = 2, max = 100, message = "Candidate name must be between 2 and 100 characters")
        private String name;
        
        @Size(max = 500, message = "Candidate description cannot exceed 500 characters")
        private String description;
        
        private Integer displayOrder;
        
        // Constructors
        public CandidateRequest() {}
        
        public CandidateRequest(String name, String description, Integer displayOrder) {
            this.name = name;
            this.description = description;
            this.displayOrder = displayOrder;
        }
        
        // Getters and Setters
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public Integer getDisplayOrder() {
            return displayOrder;
        }
        
        public void setDisplayOrder(Integer displayOrder) {
            this.displayOrder = displayOrder;
        }
        
        public boolean isValid() {
            return name != null && !name.trim().isEmpty() && name.length() >= 2 && name.length() <= 100;
        }
        
        @Override
        public String toString() {
            return "CandidateRequest{" +
                    "name='" + name + '\'' +
                    ", description='" + description + '\'' +
                    ", displayOrder=" + displayOrder +
                    '}';
        }
    }
    
    @Override
    public String toString() {
        return "ElectionRequest{" +
                "title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", candidates=" + candidates +
                '}';
    }
}