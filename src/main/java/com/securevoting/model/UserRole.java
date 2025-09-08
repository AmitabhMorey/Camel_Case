package com.securevoting.model;

/**
 * Enumeration for user roles in the voting system.
 */
public enum UserRole {
    VOTER("ROLE_VOTER"),
    ADMIN("ROLE_ADMIN");
    
    private final String authority;
    
    UserRole(String authority) {
        this.authority = authority;
    }
    
    public String getAuthority() {
        return authority;
    }
    
    @Override
    public String toString() {
        return authority;
    }
}