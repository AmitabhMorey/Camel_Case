package com.securevoting.security;

import com.securevoting.model.User;
import com.securevoting.model.UserRole;
import com.securevoting.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MultiFactorAuthenticationProvider.
 */
@ExtendWith(MockitoExtension.class)
class MultiFactorAuthenticationProviderTest {
    
    @Mock
    private AuthenticationService authenticationService;
    
    @InjectMocks
    private MultiFactorAuthenticationProvider authenticationProvider;
    
    private User testUser;
    private String testUserId;
    
    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID().toString();
        testUser = new User(testUserId, "testuser", "test@example.com", "hashedPassword");
        testUser.setRole(UserRole.VOTER);
        testUser.setEnabled(true);
    }
    
    @Test
    void testAuthenticate_Success() {
        // Arrange
        String username = "testuser";
        String password = "password123";
        
        UsernamePasswordAuthenticationToken authToken = 
            new UsernamePasswordAuthenticationToken(username, password);
        
        when(authenticationService.validateCredentials(username, password))
            .thenReturn(testUser);
        
        // Act
        Authentication result = authenticationProvider.authenticate(authToken);
        
        // Assert
        assertNotNull(result);
        assertTrue(result instanceof MultiFactorAuthenticationToken);
        
        MultiFactorAuthenticationToken mfaToken = (MultiFactorAuthenticationToken) result;
        assertEquals(testUserId, mfaToken.getUserId());
        assertEquals(username, mfaToken.getPrincipal());
        assertEquals(testUser, mfaToken.getUser());
        assertTrue(mfaToken.isFirstFactorComplete());
        assertFalse(mfaToken.isSecondFactorComplete());
        assertFalse(mfaToken.isAuthenticated()); // Not fully authenticated yet
        
        // Check authorities
        assertEquals(1, mfaToken.getAuthorities().size());
        GrantedAuthority authority = mfaToken.getAuthorities().iterator().next();
        assertEquals("ROLE_VOTER", authority.getAuthority());
        
        verify(authenticationService).validateCredentials(username, password);
    }
    
    @Test
    void testAuthenticate_AdminUser() {
        // Arrange
        String username = "admin";
        String password = "adminpass";
        testUser.setRole(UserRole.ADMIN);
        
        UsernamePasswordAuthenticationToken authToken = 
            new UsernamePasswordAuthenticationToken(username, password);
        
        when(authenticationService.validateCredentials(username, password))
            .thenReturn(testUser);
        
        // Act
        Authentication result = authenticationProvider.authenticate(authToken);
        
        // Assert
        assertNotNull(result);
        assertTrue(result instanceof MultiFactorAuthenticationToken);
        
        MultiFactorAuthenticationToken mfaToken = (MultiFactorAuthenticationToken) result;
        
        // Check authorities for admin
        assertEquals(1, mfaToken.getAuthorities().size());
        GrantedAuthority authority = mfaToken.getAuthorities().iterator().next();
        assertEquals("ROLE_ADMIN", authority.getAuthority());
    }
    
    @Test
    void testAuthenticate_InvalidCredentials() {
        // Arrange
        String username = "testuser";
        String password = "wrongpassword";
        
        UsernamePasswordAuthenticationToken authToken = 
            new UsernamePasswordAuthenticationToken(username, password);
        
        when(authenticationService.validateCredentials(username, password))
            .thenReturn(null);
        
        // Act & Assert
        BadCredentialsException exception = assertThrows(
            BadCredentialsException.class,
            () -> authenticationProvider.authenticate(authToken)
        );
        
        assertEquals("Invalid username/email or password", exception.getMessage());
        verify(authenticationService).validateCredentials(username, password);
    }
    
    @Test
    void testAuthenticate_WithEmail() {
        // Arrange
        String email = "test@example.com";
        String password = "password123";
        
        UsernamePasswordAuthenticationToken authToken = 
            new UsernamePasswordAuthenticationToken(email, password);
        
        when(authenticationService.validateCredentials(email, password))
            .thenReturn(testUser);
        
        // Act
        Authentication result = authenticationProvider.authenticate(authToken);
        
        // Assert
        assertNotNull(result);
        assertTrue(result instanceof MultiFactorAuthenticationToken);
        
        MultiFactorAuthenticationToken mfaToken = (MultiFactorAuthenticationToken) result;
        assertEquals(email, mfaToken.getPrincipal());
        assertEquals(testUser, mfaToken.getUser());
        
        verify(authenticationService).validateCredentials(email, password);
    }
    
    @Test
    void testSupports_UsernamePasswordAuthenticationToken() {
        // Act & Assert
        assertTrue(authenticationProvider.supports(UsernamePasswordAuthenticationToken.class));
    }
    
    @Test
    void testSupports_MultiFactorAuthenticationToken() {
        // Act & Assert
        assertTrue(authenticationProvider.supports(MultiFactorAuthenticationToken.class));
    }
    
    @Test
    void testSupports_UnsupportedAuthenticationType() {
        // Act & Assert
        assertFalse(authenticationProvider.supports(String.class));
    }
}