package com.securevoting.controller;

import com.securevoting.model.User;
import com.securevoting.model.UserRole;
import com.securevoting.security.MultiFactorAuthenticationProvider;
import com.securevoting.service.AuthenticationResult;
import com.securevoting.service.AuthenticationService;
import com.securevoting.service.QRCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController.
 */
@WebMvcTest(AuthController.class)
@Import({com.securevoting.config.TestSecurityConfig.class})
class AuthControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private AuthenticationService authenticationService;
    
    @MockBean
    private QRCodeService qrCodeService;
    
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
    void testLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }
    
    @Test
    void testLoginPageWithError() throws Exception {
        mockMvc.perform(get("/login").param("error", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("error", "Invalid username or password"));
    }
    
    @Test
    void testLoginPageWithLogout() throws Exception {
        mockMvc.perform(get("/login").param("logout", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("message", "You have been logged out successfully"));
    }
    
    @Test
    void testRegistrationPage() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("user"));
    }
    
    @Test
    void testRegisterUser_Success() throws Exception {
        // Arrange
        when(authenticationService.registerUser("newuser", "newuser@example.com", "password123"))
                .thenReturn(testUser);
        
        // Act & Assert
        mockMvc.perform(post("/register")
                .with(csrf())
                .param("username", "newuser")
                .param("email", "newuser@example.com")
                .param("password", "password123")
                .param("confirmPassword", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("message", "Registration successful! Please login to continue."));
        
        verify(authenticationService).registerUser("newuser", "newuser@example.com", "password123");
    }
    
    @Test
    void testRegisterUser_UsernameExists() throws Exception {
        // Arrange
        when(authenticationService.registerUser("existinguser", "newuser@example.com", "password123"))
                .thenThrow(new IllegalArgumentException("Username already exists"));
        
        // Act & Assert
        mockMvc.perform(post("/register")
                .with(csrf())
                .param("username", "existinguser")
                .param("email", "newuser@example.com")
                .param("password", "password123")
                .param("confirmPassword", "password123"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().hasErrors());
    }
    
    @Test
    @WithMockUser
    void testQRAuthPage_NotInMFAState() throws Exception {
        mockMvc.perform(get("/qr-auth"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
    
    @Test
    void testValidateQRCode_Success() throws Exception {
        // Arrange
        String qrData = "test-qr-data";
        AuthenticationResult successResult = AuthenticationResult.success(null, testUserId, null);
        
        when(authenticationService.authenticateWithQR(testUserId, qrData))
                .thenReturn(successResult);
        
        // Act & Assert
        mockMvc.perform(post("/qr-auth")
                .with(csrf())
                .param("qrData", qrData)
                .param("userId", testUserId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/otp-auth"))
                .andExpect(flash().attribute("message", "QR code validated. Please enter your OTP."))
                .andExpect(flash().attribute("userId", testUserId));
        
        verify(authenticationService).authenticateWithQR(testUserId, qrData);
    }
    
    @Test
    void testValidateQRCode_Failure() throws Exception {
        // Arrange
        String qrData = "invalid-qr-data";
        AuthenticationResult failureResult = AuthenticationResult.failure("Invalid QR code");
        
        when(authenticationService.authenticateWithQR(testUserId, qrData))
                .thenReturn(failureResult);
        
        // Act & Assert
        mockMvc.perform(post("/qr-auth")
                .with(csrf())
                .param("qrData", qrData)
                .param("userId", testUserId))
                .andExpect(status().isOk())
                .andExpect(view().name("qr-auth"))
                .andExpect(model().attribute("error", "Invalid QR code"))
                .andExpect(model().attribute("userId", testUserId));
        
        verify(authenticationService).authenticateWithQR(testUserId, qrData);
    }
    
    @Test
    void testOTPAuthPage_WithUserId() throws Exception {
        mockMvc.perform(get("/otp-auth")
                .flashAttr("userId", testUserId))
                .andExpect(status().isOk())
                .andExpect(view().name("otp-auth"))
                .andExpect(model().attribute("userId", testUserId));
    }
    
    @Test
    void testOTPAuthPage_WithoutUserId() throws Exception {
        mockMvc.perform(get("/otp-auth"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
    
    @Test
    void testValidateOTP_Success() throws Exception {
        // Arrange
        String otp = "123456";
        String sessionId = UUID.randomUUID().toString();
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(30);
        
        AuthenticationResult successResult = AuthenticationResult.success(sessionId, testUserId, expiryTime);
        
        when(authenticationService.authenticateWithOTP(testUserId, otp))
                .thenReturn(successResult);
        
        // Act & Assert
        mockMvc.perform(post("/otp-auth")
                .with(csrf())
                .param("otp", otp)
                .param("userId", testUserId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
        
        verify(authenticationService).authenticateWithOTP(testUserId, otp);
    }
    
    @Test
    void testValidateOTP_Failure() throws Exception {
        // Arrange
        String otp = "invalid";
        AuthenticationResult failureResult = AuthenticationResult.failure("Invalid OTP");
        
        when(authenticationService.authenticateWithOTP(testUserId, otp))
                .thenReturn(failureResult);
        
        // Act & Assert
        mockMvc.perform(post("/otp-auth")
                .with(csrf())
                .param("otp", otp)
                .param("userId", testUserId))
                .andExpect(status().isOk())
                .andExpect(view().name("otp-auth"))
                .andExpect(model().attribute("error", "Invalid OTP"))
                .andExpect(model().attribute("userId", testUserId));
        
        verify(authenticationService).authenticateWithOTP(testUserId, otp);
    }
}