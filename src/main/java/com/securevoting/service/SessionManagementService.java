package com.securevoting.service;

import com.securevoting.model.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing user sessions with automatic cleanup of expired sessions.
 */
@Service
public class SessionManagementService {
    
    private static final Logger logger = LoggerFactory.getLogger(SessionManagementService.class);
    
    @Value("${app.session.timeout.minutes:30}")
    private int sessionTimeoutMinutes;
    
    @Value("${app.session.cleanup.interval.minutes:5}")
    private int cleanupIntervalMinutes;
    
    // In-memory session storage (in production, use Redis or database)
    private final Map<String, UserSession> activeSessions = new ConcurrentHashMap<>();
    
    /**
     * Stores a session in the session store.
     */
    public void storeSession(UserSession session) {
        activeSessions.put(session.getSessionId(), session);
        logger.debug("Stored session: {} for user: {}", session.getSessionId(), session.getUserId());
    }
    
    /**
     * Retrieves a session by session ID.
     */
    public UserSession getSession(String sessionId) {
        return activeSessions.get(sessionId);
    }
    
    /**
     * Removes a session from the session store.
     */
    public void removeSession(String sessionId) {
        UserSession session = activeSessions.remove(sessionId);
        if (session != null) {
            logger.debug("Removed session: {} for user: {}", sessionId, session.getUserId());
        }
    }
    
    /**
     * Checks if a session exists and is valid.
     */
    public boolean isSessionValid(String sessionId) {
        UserSession session = activeSessions.get(sessionId);
        if (session == null) {
            return false;
        }
        
        if (session.isExpired()) {
            // Remove expired session
            activeSessions.remove(sessionId);
            logger.debug("Removed expired session: {}", sessionId);
            return false;
        }
        
        return session.isValid();
    }
    
    /**
     * Gets the user ID associated with a valid session.
     */
    public String getUserIdFromSession(String sessionId) {
        UserSession session = activeSessions.get(sessionId);
        if (session != null && session.isValid() && !session.isExpired()) {
            return session.getUserId();
        }
        return null;
    }
    
    /**
     * Extends the expiry time of a session.
     */
    public void extendSession(String sessionId) {
        UserSession session = activeSessions.get(sessionId);
        if (session != null && session.isValid()) {
            LocalDateTime newExpiryTime = LocalDateTime.now().plusMinutes(sessionTimeoutMinutes);
            session.setExpiryTime(newExpiryTime);
            logger.debug("Extended session: {} until: {}", sessionId, newExpiryTime);
        }
    }
    
    /**
     * Gets the count of active sessions.
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }
    
    /**
     * Gets the count of sessions for a specific user.
     */
    public long getSessionCountForUser(String userId) {
        return activeSessions.values().stream()
                .filter(session -> userId.equals(session.getUserId()) && session.isValid())
                .count();
    }
    
    /**
     * Invalidates all sessions for a specific user.
     */
    public void invalidateUserSessions(String userId) {
        activeSessions.entrySet().removeIf(entry -> {
            UserSession session = entry.getValue();
            if (userId.equals(session.getUserId())) {
                session.setActive(false);
                logger.debug("Invalidated session: {} for user: {}", entry.getKey(), userId);
                return true;
            }
            return false;
        });
    }
    
    /**
     * Scheduled task to cleanup expired sessions.
     * Runs every 5 minutes by default.
     */
    @Scheduled(fixedRateString = "${app.session.cleanup.interval.minutes:5}000")
    public void cleanupExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        int removedCount = 0;
        
        var iterator = activeSessions.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            UserSession session = entry.getValue();
            
            if (session.isExpired() || !session.isActive()) {
                iterator.remove();
                removedCount++;
                logger.debug("Cleaned up expired/inactive session: {}", entry.getKey());
            }
        }
        
        if (removedCount > 0) {
            logger.info("Cleaned up {} expired/inactive sessions. Active sessions: {}", 
                       removedCount, activeSessions.size());
        }
    }
}