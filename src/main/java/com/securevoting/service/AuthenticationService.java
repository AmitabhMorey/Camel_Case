package com.securevoting.service;

import com.securevoting.model.User;

/**
 * Service interface for user authentication and session management.
 * Provides multi-factor authentication using QR codes and OTPs.
 */
public interface AuthenticationService {
    
    /**
     * Authenticates a user using QR code validation.
     * 
     * @param userId the user ID attempting authentication
     * @param qrData the QR code data provided by the user
     * @return authentication result containing success status and session details
     */
    AuthenticationResult authenticateWithQR(String userId, String qrData);
    
    /**
     * Authenticates a user using OTP validation.
     * 
     * @param userId the user ID attempting authentication
     * @param otp the OTP provided by the user
     * @return authentication result containing success status and session details
     */
    AuthenticationResult authenticateWithOTP(String userId, String otp);
    
    /**
     * Creates a secure session for the authenticated user.
     * 
     * @param userId the user ID for whom to create the session
     * @return session ID for the created session
     */
    String createUserSession(String userId);
    
    /**
     * Invalidates a user session.
     * 
     * @param sessionId the session ID to invalidate
     */
    void invalidateSession(String sessionId);
    
    /**
     * Validates if a session is still active and not expired.
     * 
     * @param sessionId the session ID to validate
     * @return true if session is valid, false otherwise
     */
    boolean isSessionValid(String sessionId);
    
    /**
     * Gets the user ID associated with a session.
     * 
     * @param sessionId the session ID
     * @return user ID if session is valid, null otherwise
     */
    String getUserIdFromSession(String sessionId);
    
    /**
     * Registers a new user with password hashing and QR code generation.
     * 
     * @param username the username for the new user
     * @param email the email for the new user
     * @param password the plain text password to be hashed
     * @return the created user entity
     */
    User registerUser(String username, String email, String password);
    
    /**
     * Validates user credentials (username/email and password).
     * 
     * @param identifier username or email
     * @param password plain text password
     * @return user entity if credentials are valid, null otherwise
     */
    User validateCredentials(String identifier, String password);
}