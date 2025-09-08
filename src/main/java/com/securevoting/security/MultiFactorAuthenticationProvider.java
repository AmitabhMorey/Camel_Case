package com.securevoting.security;

import com.securevoting.model.User;
import com.securevoting.service.AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom Spring Security authentication provider for multi-factor authentication.
 * Handles username/password validation as the first factor of authentication.
 */
@Component
public class MultiFactorAuthenticationProvider implements AuthenticationProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(MultiFactorAuthenticationProvider.class);
    
    @Autowired
    private AuthenticationService authenticationService;
    
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String identifier = authentication.getName();
        String password = authentication.getCredentials().toString();
        
        logger.debug("Attempting authentication for identifier: {}", identifier);
        
        // Validate credentials using AuthenticationService
        User user = authenticationService.validateCredentials(identifier, password);
        
        if (user == null) {
            logger.warn("Authentication failed for identifier: {}", identifier);
            throw new BadCredentialsException("Invalid username/email or password");
        }
        
        // Create authorities based on user role
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        
        // Create custom authentication token with user details
        MultiFactorAuthenticationToken authToken = new MultiFactorAuthenticationToken(
            user.getUserId(), 
            identifier, // Use original identifier (username or email)
            password, 
            authorities
        );
        authToken.setUser(user);
        authToken.setFirstFactorComplete(true);
        
        logger.info("First factor authentication successful for user: {}", user.getUsername());
        return authToken;
    }
    
    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication) ||
               MultiFactorAuthenticationToken.class.isAssignableFrom(authentication);
    }
}