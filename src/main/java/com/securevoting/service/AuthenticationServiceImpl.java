package com.securevoting.service;

import com.securevoting.exception.OTPGenerationException;
import com.securevoting.exception.QRCodeGenerationException;
import com.securevoting.model.User;
import com.securevoting.model.UserRole;
import com.securevoting.model.UserSession;
import com.securevoting.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of AuthenticationService providing multi-factor authentication
 * and session management functionality.
 */
@Service
@Transactional
public class AuthenticationServiceImpl implements AuthenticationService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationServiceImpl.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private QRCodeService qrCodeService;
    
    @Autowired
    private OTPService otpService;
    
    @Value("${app.session.timeout.minutes:30}")
    private int sessionTimeoutMinutes;
    
    // In-memory session storage (in production, use Redis or database)
    private final Map<String, UserSession> activeSessions = new ConcurrentHashMap<>();
    
    @Override
    public AuthenticationResult authenticateWithQR(String userId, String qrData) {
        logger.info("Attempting QR authentication for user: {}", userId);
        
        try {
            // Validate user exists and is enabled
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty() || !userOpt.get().isEnabled()) {
                logger.warn("QR authentication failed: User not found or disabled - {}", userId);
                return AuthenticationResult.failure("User not found or account disabled");
            }
            
            // Validate QR code
            if (!qrCodeService.validateQRCodeData(qrData, userId)) {
                logger.warn("QR authentication failed: Invalid QR code for user - {}", userId);
                return AuthenticationResult.failure("Invalid or expired QR code");
            }
            
            // Generate OTP for second factor
            String otp = otpService.generateOTP(userId);
            logger.info("QR authentication successful for user: {}, OTP generated", userId);
            
            // Return partial success - user still needs to provide OTP
            return AuthenticationResult.success(null, userId, null);
            
        } catch (QRCodeGenerationException e) {
            logger.error("QR authentication failed due to QR code validation error for user: {}", userId, e);
            return AuthenticationResult.failure("QR code validation failed");
        } catch (OTPGenerationException e) {
            logger.error("QR authentication failed due to OTP generation error for user: {}", userId, e);
            return AuthenticationResult.failure("Failed to generate OTP");
        } catch (Exception e) {
            logger.error("Unexpected error during QR authentication for user: {}", userId, e);
            return AuthenticationResult.failure("Authentication failed");
        }
    }
    
    @Override
    public AuthenticationResult authenticateWithOTP(String userId, String otp) {
        logger.info("Attempting OTP authentication for user: {}", userId);
        
        try {
            // Validate user exists and is enabled
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty() || !userOpt.get().isEnabled()) {
                logger.warn("OTP authentication failed: User not found or disabled - {}", userId);
                return AuthenticationResult.failure("User not found or account disabled");
            }
            
            // Validate OTP
            if (!otpService.validateOTP(userId, otp)) {
                logger.warn("OTP authentication failed: Invalid OTP for user - {}", userId);
                return AuthenticationResult.failure("Invalid or expired OTP");
            }
            
            // Create session for successful authentication
            String sessionId = createUserSession(userId);
            LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(sessionTimeoutMinutes);
            
            logger.info("OTP authentication successful for user: {}, session created: {}", userId, sessionId);
            return AuthenticationResult.success(sessionId, userId, expiryTime);
            
        } catch (Exception e) {
            logger.error("Unexpected error during OTP authentication for user: {}", userId, e);
            return AuthenticationResult.failure("Authentication failed");
        }
    }
    
    @Override
    public String createUserSession(String userId) {
        String sessionId = UUID.randomUUID().toString();
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(sessionTimeoutMinutes);
        
        UserSession session = new UserSession(sessionId, userId, expiryTime);
        activeSessions.put(sessionId, session);
        
        logger.info("Created session {} for user {} with expiry {}", sessionId, userId, expiryTime);
        return sessionId;
    }
    
    @Override
    public void invalidateSession(String sessionId) {
        UserSession session = activeSessions.remove(sessionId);
        if (session != null) {
            session.setActive(false);
            logger.info("Invalidated session {} for user {}", sessionId, session.getUserId());
        }
    }
    
    @Override
    public boolean isSessionValid(String sessionId) {
        UserSession session = activeSessions.get(sessionId);
        if (session == null) {
            return false;
        }
        
        if (session.isExpired()) {
            // Remove expired session
            activeSessions.remove(sessionId);
            logger.info("Removed expired session {}", sessionId);
            return false;
        }
        
        return session.isValid();
    }
    
    @Override
    public String getUserIdFromSession(String sessionId) {
        UserSession session = activeSessions.get(sessionId);
        if (session != null && session.isValid()) {
            return session.getUserId();
        }
        return null;
    }
    
    @Override
    public User registerUser(String username, String email, String password) {
        logger.info("Registering new user with username: {} and email: {}", username, email);
        
        // Check if username or email already exists
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }
        
        // Create new user
        String userId = UUID.randomUUID().toString();
        String hashedPassword = passwordEncoder.encode(password);
        
        User user = new User(userId, username, email, hashedPassword);
        user.setRole(UserRole.VOTER);
        user.setEnabled(true);
        
        // Generate QR code secret for the user
        try {
            String qrSecret = generateQRSecret(userId);
            user.setQrCodeSecret(qrSecret);
        } catch (Exception e) {
            logger.warn("Failed to generate QR secret for user {}, proceeding without QR code", username, e);
        }
        
        User savedUser = userRepository.save(user);
        logger.info("Successfully registered user: {} with ID: {}", username, userId);
        
        return savedUser;
    }
    
    @Override
    public User validateCredentials(String identifier, String password) {
        logger.debug("Validating credentials for identifier: {}", identifier);
        
        Optional<User> userOpt = userRepository.findByUsernameOrEmail(identifier);
        if (userOpt.isEmpty()) {
            logger.warn("User not found for identifier: {}", identifier);
            return null;
        }
        
        User user = userOpt.get();
        if (!user.isEnabled()) {
            logger.warn("Account disabled for user: {}", user.getUsername());
            return null;
        }
        
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            logger.warn("Invalid password for user: {}", user.getUsername());
            return null;
        }
        
        logger.info("Credentials validated successfully for user: {}", user.getUsername());
        return user;
    }
    
    /**
     * Generates a QR code secret for the user.
     * This is a simplified implementation - in production, use proper secret generation.
     */
    private String generateQRSecret(String userId) {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
    
    /**
     * Cleanup expired sessions periodically.
     * This method should be called by a scheduled task.
     */
    public void cleanupExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        activeSessions.entrySet().removeIf(entry -> {
            UserSession session = entry.getValue();
            if (session.isExpired()) {
                logger.debug("Removing expired session: {}", entry.getKey());
                return true;
            }
            return false;
        });
    }
}