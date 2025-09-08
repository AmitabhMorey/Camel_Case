package com.securevoting.exception;

/**
 * Exception thrown when voting operations fail.
 * This includes vote casting failures, validation errors, and election-related issues.
 */
public class VotingException extends RuntimeException {
    
    private final String errorCode;
    
    /**
     * Constructs a new VotingException with the specified detail message.
     * 
     * @param message the detail message
     */
    public VotingException(String message) {
        super(message);
        this.errorCode = "VOTING_ERROR";
    }
    
    /**
     * Constructs a new VotingException with the specified detail message and error code.
     * 
     * @param message the detail message
     * @param errorCode the error code for categorization
     */
    public VotingException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    /**
     * Constructs a new VotingException with the specified detail message and cause.
     * 
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public VotingException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "VOTING_ERROR";
    }
    
    /**
     * Constructs a new VotingException with the specified detail message, cause, and error code.
     * 
     * @param message the detail message
     * @param cause the cause of the exception
     * @param errorCode the error code for categorization
     */
    public VotingException(String message, Throwable cause, String errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    /**
     * Gets the error code associated with this exception.
     * 
     * @return the error code
     */
    public String getErrorCode() {
        return errorCode;
    }
}