package com.securevoting.model;

/**
 * Enumeration for audit event types in the voting system.
 */
public enum AuditEventType {
    USER_REGISTRATION("User Registration"),
    USER_LOGIN("User Login"),
    USER_LOGOUT("User Logout"),
    LOGIN_FAILED("Login Failed"),
    QR_CODE_GENERATED("QR Code Generated"),
    QR_CODE_SCANNED("QR Code Scanned"),
    OTP_GENERATED("OTP Generated"),
    OTP_VALIDATED("OTP Validated"),
    OTP_FAILED("OTP Failed"),
    VOTE_CAST("Vote Cast"),
    VOTE_FAILED("Vote Failed"),
    ELECTION_CREATED("Election Created"),
    ELECTION_UPDATED("Election Updated"),
    ELECTION_STARTED("Election Started"),
    ELECTION_ENDED("Election Ended"),
    RESULTS_VIEWED("Results Viewed"),
    ADMIN_ACTION("Admin Action"),
    SECURITY_EVENT("Security Event"),
    SYSTEM_ERROR("System Error"),
    DATA_ACCESS("Data Access"),
    CONFIGURATION_CHANGE("Configuration Change"),
    USER_ACTION("User Action"),
    AUTH_SUCCESS("Authentication Success"),
    AUTH_FAILURE("Authentication Failure"),
    VOTE_ATTEMPT("Vote Attempt"),
    VOTE_SUCCESS("Vote Success"),
    SECURITY_VIOLATION("Security Violation");
    
    private final String displayName;
    
    AuditEventType(String displayName) {
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