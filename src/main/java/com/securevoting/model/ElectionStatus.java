package com.securevoting.model;

/**
 * Enumeration for election status in the voting system.
 */
public enum ElectionStatus {
    DRAFT("Draft"),
    ACTIVE("Active"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");
    
    private final String displayName;
    
    ElectionStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}