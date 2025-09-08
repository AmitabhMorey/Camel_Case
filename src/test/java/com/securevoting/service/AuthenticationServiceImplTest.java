package com.securevoting.service;

import com.securevoting.exception.OTPGenerationException;
import com.securevoting.exception.QRCodeGenerationException;
import com.securevoting.model.User;
import com.securevoting.model.UserRole;
import com.securevoting.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for AuthenticationServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private QRCodeService qrCodeService;
    
    @Mock
    private OTPService otpService;
    
    @InjectMocks
    private AuthenticationServiceImpl authenticationService;
    
    private User testUser;
    private String testUserId;
    
    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID().toString();
        testUser = new User(testUserId, "testuser", "test@example.com", "hashedPassword");
        testUser.setRole(UserRole.VOTER);
        testUser.setEnabled(true);
        testUser.setQrCodeSecret("test-qr-secret");
        
        // Set session timeout for testing
        ReflectionTestUtils.setField(authenticationService, "sessionTimeoutMinutes", 30);
    }
    
    @Test
    void testAuthenticateWithQR_Success() {
        // Arrange
        String qrData = "test-qr-data";
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(qrCodeService.validateQRCodeData(qrData, testUserId)).thenReturn(true);
        when(otpService.generateOTP(testUserId)).thenReturn("123456");
        
        // Act
        AuthenticationResult result = authenticationService.authenticateWithQR(testUserId, qrData);
        
        // Assert
        assertTrue(result.isSuccessful());
        assertEquals(testUserId, result.getUserId());
        assertNull(result.getSessionId()); // No session created yet, waiting for OTP
        verify(qrCodeService).validateQRCodeData(qrData, testUserId);
        verify(otpService).generateOTP(testUserId);
    }
    
    @Test
    void testAuthenticateWithQR_UserNotFound() {
        // Arrange
        String qrData = "test-qr-data";
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());
        
        // Act
        AuthenticationResult result = authenticationService.authenticateWithQR(testUserId, qrData);
        
        // Assert
        assertFalse(result.isSuccessful());
        assertEquals("User not found or account disabled", result.getErrorMessage());
        verify(qrCodeService, never()).validateQRCodeData(any(), any());
    }
    
    @Test
    void testAuthenticateWithQR_UserDisabled() {
        // Arrange
        String qrData = "test-qr-data";
        testUser.setEnabled(false);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        
        // Act
        AuthenticationResult result = authenticationService.authenticateWithQR(testUserId, qrData);
        
        // Assert
        assertFalse(result.isSuccessful());
        assertEquals("User not found or account disabled", result.getErrorMessage());
    }
    
    @Test
    void testAuthenticateWithQR_InvalidQRCode() {
        // Arrange
        String qrData = "invalid-qr-data";
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(qrCodeService.validateQRCodeData(qrData, testUserId)).thenReturn(false);
        
        // Act
        AuthenticationResult result = authenticationService.authenticateWithQR(testUserId, qrData);
        
        // Assert
        assertFalse(result.isSuccessful());
        assertEquals("Invalid or expired QR code", result.getErrorMessage());
        verify(otpService, never()).generateOTP(any());
    }
    
    @Test
    void testAuthenticateWithQR_QRCodeException() {
        // Arrange
        String qrData = "test-qr-data";
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(qrCodeService.validateQRCodeData(qrData, testUserId))
            .thenThrow(new QRCodeGenerationException("QR validation failed"));
        
        // Act
        AuthenticationResult result = authenticationService.authenticateWithQR(testUserId, qrData);
        
        // Assert
        assertFalse(result.isSuccessful());
        assertEquals("QR code validation failed", result.getErrorMessage());
    }
    
    @Test
    void testAuthenticateWithOTP_Success() {
        // Arrange
        String otp = "123456";
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(otpService.validateOTP(testUserId, otp)).thenReturn(true);
        
        // Act
        AuthenticationResult result = authenticationService.authenticateWithOTP(testUserId, otp);
        
        // Assert
        assertTrue(result.isSuccessful());
        assertEquals(testUserId, result.getUserId());
        assertNotNull(result.getSessionId());
        assertNotNull(result.getExpiryTime());
        assertTrue(result.getExpiryTime().isAfter(LocalDateTime.now()));
    }
    
    @Test
    void testAuthenticateWithOTP_InvalidOTP() {
        // Arrange
        String otp = "invalid";
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(otpService.validateOTP(testUserId, otp)).thenReturn(false);
        
        // Act
        AuthenticationResult result = authenticationService.authenticateWithOTP(testUserId, otp);
        
        // Assert
        assertFalse(result.isSuccessful());
        assertEquals("Invalid or expired OTP", result.getErrorMessage());
    }
    
    @Test
    void testRegisterUser_Success() {
        // Arrange
        String username = "newuser";
        String email = "newuser@example.com";
        String password = "password123";
        String hashedPassword = "hashedPassword123";
        
        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn(hashedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        User result = authenticationService.registerUser(username, email, password);
        
        // Assert
        assertNotNull(result);
        assertEquals(username, result.getUsername());
        assertEquals(email, result.getEmail());
        assertEquals(hashedPassword, result.getPasswordHash());
        assertEquals(UserRole.VOTER, result.getRole());
        assertTrue(result.isEnabled());
        assertNotNull(result.getUserId());
        assertNotNull(result.getQrCodeSecret());
        
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    void testRegisterUser_UsernameExists() {
        // Arrange
        String username = "existinguser";
        String email = "newuser@example.com";
        String password = "password123";
        
        when(userRepository.existsByUsername(username)).thenReturn(true);
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> authenticationService.registerUser(username, email, password)
        );
        assertEquals("Username already exists", exception.getMessage());
        
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void testRegisterUser_EmailExists() {
        // Arrange
        String username = "newuser";
        String email = "existing@example.com";
        String password = "password123";
        
        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(userRepository.existsByEmail(email)).thenReturn(true);
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> authenticationService.registerUser(username, email, password)
        );
        assertEquals("Email already exists", exception.getMessage());
        
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void testValidateCredentials_Success() {
        // Arrange
        String identifier = "testuser";
        String password = "password123";
        
        when(userRepository.findByUsernameOrEmail(identifier)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(password, testUser.getPasswordHash())).thenReturn(true);
        
        // Act
        User result = authenticationService.validateCredentials(identifier, password);
        
        // Assert
        assertNotNull(result);
        assertEquals(testUser.getUserId(), result.getUserId());
        assertEquals(testUser.getUsername(), result.getUsername());
    }
    
    @Test
    void testValidateCredentials_UserNotFound() {
        // Arrange
        String identifier = "nonexistent";
        String password = "password123";
        
        when(userRepository.findByUsernameOrEmail(identifier)).thenReturn(Optional.empty());
        
        // Act
        User result = authenticationService.validateCredentials(identifier, password);
        
        // Assert
        assertNull(result);
    }
    
    @Test
    void testValidateCredentials_UserDisabled() {
        // Arrange
        String identifier = "testuser";
        String password = "password123";
        testUser.setEnabled(false);
        
        when(userRepository.findByUsernameOrEmail(identifier)).thenReturn(Optional.of(testUser));
        
        // Act
        User result = authenticationService.validateCredentials(identifier, password);
        
        // Assert
        assertNull(result);
    }
    
    @Test
    void testValidateCredentials_InvalidPassword() {
        // Arrange
        String identifier = "testuser";
        String password = "wrongpassword";
        
        when(userRepository.findByUsernameOrEmail(identifier)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(password, testUser.getPasswordHash())).thenReturn(false);
        
        // Act
        User result = authenticationService.validateCredentials(identifier, password);
        
        // Assert
        assertNull(result);
    }
    
    @Test
    void testSessionManagement() {
        // Test session creation
        String sessionId = authenticationService.createUserSession(testUserId);
        assertNotNull(sessionId);
        
        // Test session validation
        assertTrue(authenticationService.isSessionValid(sessionId));
        assertEquals(testUserId, authenticationService.getUserIdFromSession(sessionId));
        
        // Test session invalidation
        authenticationService.invalidateSession(sessionId);
        assertFalse(authenticationService.isSessionValid(sessionId));
        assertNull(authenticationService.getUserIdFromSession(sessionId));
    }
}