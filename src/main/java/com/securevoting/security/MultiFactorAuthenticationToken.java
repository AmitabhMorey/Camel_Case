package com.securevoting.security;

import com.securevoting.model.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * Custom authentication token for multi-factor authentication.
 * Extends UsernamePasswordAuthenticationToken to include additional MFA state.
 */
public class MultiFactorAuthenticationToken extends UsernamePasswordAuthenticationToken {
    
    private User user;
    private String userId;
    private boolean firstFactorComplete = false;
    private boolean secondFactorComplete = false;
    private String sessionId;
    
    // Constructor for unauthenticated token
    public MultiFactorAuthenticationToken(String principal, Object credentials) {
        super(principal, credentials);
    }
    
    // Constructor for authenticated token
    public MultiFactorAuthenticationToken(String userId, String principal, Object credentials, 
                                        Collection<? extends GrantedAuthority> authorities) {
        super(principal, credentials, authorities);
        this.userId = userId;
    }
    
    // Constructor for fully authenticated token with session
    public MultiFactorAuthenticationToken(String userId, String principal, Object credentials,
                                        Collection<? extends GrantedAuthority> authorities, 
                                        String sessionId) {
        super(principal, credentials, authorities);
        this.userId = userId;
        this.sessionId = sessionId;
        this.firstFactorComplete = true;
        this.secondFactorComplete = true;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public boolean isFirstFactorComplete() {
        return firstFactorComplete;
    }
    
    public void setFirstFactorComplete(boolean firstFactorComplete) {
        this.firstFactorComplete = firstFactorComplete;
    }
    
    public boolean isSecondFactorComplete() {
        return secondFactorComplete;
    }
    
    public void setSecondFactorComplete(boolean secondFactorComplete) {
        this.secondFactorComplete = secondFactorComplete;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    /**
     * Returns true only if both authentication factors are complete.
     */
    @Override
    public boolean isAuthenticated() {
        return super.isAuthenticated() && firstFactorComplete && secondFactorComplete;
    }
    
    /**
     * Checks if the user has completed the first factor (username/password).
     */
    public boolean isPartiallyAuthenticated() {
        return firstFactorComplete && !secondFactorComplete;
    }
    
    /**
     * Checks if the user is fully authenticated (both factors complete).
     */
    public boolean isFullyAuthenticated() {
        return firstFactorComplete && secondFactorComplete;
    }
    
    @Override
    public String toString() {
        return "MultiFactorAuthenticationToken{" +
                "userId='" + userId + '\'' +
                ", firstFactorComplete=" + firstFactorComplete +
                ", secondFactorComplete=" + secondFactorComplete +
                ", sessionId='" + sessionId + '\'' +
                ", principal=" + getPrincipal() +
                ", authenticated=" + isAuthenticated() +
                '}';
    }
}