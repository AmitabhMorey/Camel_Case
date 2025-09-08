package com.securevoting.controller;

import com.securevoting.exception.CryptographyException;
import com.securevoting.exception.OTPGenerationException;
import com.securevoting.exception.QRCodeGenerationException;
import com.securevoting.exception.VotingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GlobalExceptionHandler.
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {
    
    @Mock
    private HttpServletRequest request;
    
    @InjectMocks
    private GlobalExceptionHandler exceptionHandler;
    
    @BeforeEach
    void setUp() {
        when(request.getRequestURI()).thenReturn("/test/path");
    }
    
    @Test
    void testHandleVotingException() {
        // Arrange
        VotingException exception = new VotingException("Vote casting failed");
        when(request.getRequestURI()).thenReturn("/voting/cast-vote");
        
        // Act
        ModelAndView result = exceptionHandler.handleVotingException(exception, request);
        
        // Assert
        assertEquals("redirect:/voting/elections", result.getViewName());
        assertEquals("Vote casting failed", result.getModel().get("error"));
        assertEquals("VOTING_ERROR", result.getModel().get("errorCode"));
        assertNotNull(result.getModel().get("timestamp"));
        assertEquals("/voting/cast-vote", result.getModel().get("path"));
    }
    
    @Test
    void testHandleVotingException_NonVotingPath() {
        // Arrange
        VotingException exception = new VotingException("General voting error");
        when(request.getRequestURI()).thenReturn("/other/path");
        
        // Act
        ModelAndView result = exceptionHandler.handleVotingException(exception, request);
        
        // Assert
        assertEquals("error/error", result.getViewName());
        assertEquals("General voting error", result.getModel().get("error"));
    }
    
    @Test
    void testHandleCryptographyException() {
        // Arrange
        CryptographyException exception = new CryptographyException("Encryption failed");
        
        // Act
        ModelAndView result = exceptionHandler.handleCryptographyException(exception, request);
        
        // Assert
        assertEquals("error/error", result.getViewName());
        assertEquals("A security error occurred. Please try again.", result.getModel().get("error"));
        assertEquals("SECURITY_ERROR", result.getModel().get("errorCode"));
        assertNotNull(result.getModel().get("timestamp"));
        assertEquals("/test/path", result.getModel().get("path"));
    }
    
    @Test
    void testHandleQRCodeException_QRAuthPath() {
        // Arrange
        QRCodeGenerationException exception = new QRCodeGenerationException("QR generation failed");
        when(request.getRequestURI()).thenReturn("/qr-auth");
        
        // Act
        ModelAndView result = exceptionHandler.handleQRCodeException(exception, request);
        
        // Assert
        assertEquals("redirect:/login?error=qr", result.getViewName());
        assertEquals("Failed to generate QR code. Please try again.", result.getModel().get("error"));
        assertEquals("QR_CODE_ERROR", result.getModel().get("errorCode"));
    }
    
    @Test
    void testHandleQRCodeException_OtherPath() {
        // Arrange
        QRCodeGenerationException exception = new QRCodeGenerationException("QR generation failed");
        when(request.getRequestURI()).thenReturn("/other/path");
        
        // Act
        ModelAndView result = exceptionHandler.handleQRCodeException(exception, request);
        
        // Assert
        assertEquals("error/error", result.getViewName());
        assertEquals("Failed to generate QR code. Please try again.", result.getModel().get("error"));
    }
    
    @Test
    void testHandleOTPException_OTPAuthPath() {
        // Arrange
        OTPGenerationException exception = new OTPGenerationException("OTP generation failed");
        when(request.getRequestURI()).thenReturn("/otp-auth");
        
        // Act
        ModelAndView result = exceptionHandler.handleOTPException(exception, request);
        
        // Assert
        assertEquals("redirect:/qr-auth?error=otp", result.getViewName());
        assertEquals("Failed to generate OTP. Please try again.", result.getModel().get("error"));
        assertEquals("OTP_ERROR", result.getModel().get("errorCode"));
    }
    
    @Test
    void testHandleOTPException_OtherPath() {
        // Arrange
        OTPGenerationException exception = new OTPGenerationException("OTP generation failed");
        when(request.getRequestURI()).thenReturn("/other/path");
        
        // Act
        ModelAndView result = exceptionHandler.handleOTPException(exception, request);
        
        // Assert
        assertEquals("error/error", result.getViewName());
        assertEquals("Failed to generate OTP. Please try again.", result.getModel().get("error"));
    }
    
    @Test
    void testHandleConstraintViolationException() {
        // Arrange
        Set<ConstraintViolation<?>> violations = new HashSet<>();
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getPropertyPath()).thenReturn(mock(jakarta.validation.Path.class));
        when(violation.getPropertyPath().toString()).thenReturn("candidateId");
        when(violation.getMessage()).thenReturn("Candidate ID is required");
        violations.add(violation);
        
        ConstraintViolationException exception = new ConstraintViolationException("Validation failed", violations);
        when(request.getRequestURI()).thenReturn("/voting/cast-vote");
        
        // Act
        ModelAndView result = exceptionHandler.handleConstraintViolationException(exception, request);
        
        // Assert
        assertEquals("redirect:/voting/elections", result.getViewName());
        assertEquals("Invalid input parameters. Please check your input and try again.", result.getModel().get("error"));
        assertEquals("PARAMETER_ERROR", result.getModel().get("errorCode"));
        assertNotNull(result.getModel().get("validationErrors"));
    }
    
    @Test
    void testHandleMethodArgumentTypeMismatchException() {
        // Arrange
        MethodArgumentTypeMismatchException exception = mock(MethodArgumentTypeMismatchException.class);
        when(exception.getName()).thenReturn("electionId");
        when(exception.getMessage()).thenReturn("Type mismatch");
        when(request.getRequestURI()).thenReturn("/voting/election/invalid");
        
        // Act
        ModelAndView result = exceptionHandler.handleTypeMismatchException(exception, request);
        
        // Assert
        assertEquals("redirect:/voting/elections", result.getViewName());
        assertEquals("Invalid parameter format: electionId", result.getModel().get("error"));
        assertEquals("TYPE_MISMATCH", result.getModel().get("errorCode"));
    }
    
    @Test
    void testHandleIllegalArgumentException_RegisterPath() {
        // Arrange
        IllegalArgumentException exception = new IllegalArgumentException("Username already exists");
        when(request.getRequestURI()).thenReturn("/register");
        
        // Act
        ModelAndView result = exceptionHandler.handleIllegalArgumentException(exception, request);
        
        // Assert
        assertEquals("register", result.getViewName());
        assertEquals("Username already exists", result.getModel().get("error"));
        assertEquals("INVALID_ARGUMENT", result.getModel().get("errorCode"));
    }
    
    @Test
    void testHandleIllegalArgumentException_VotingPath() {
        // Arrange
        IllegalArgumentException exception = new IllegalArgumentException("Invalid election");
        when(request.getRequestURI()).thenReturn("/voting/election/123");
        
        // Act
        ModelAndView result = exceptionHandler.handleIllegalArgumentException(exception, request);
        
        // Assert
        assertEquals("redirect:/voting/elections", result.getViewName());
        assertEquals("Invalid election", result.getModel().get("error"));
    }
    
    @Test
    void testHandleSecurityException() {
        // Arrange
        SecurityException exception = new SecurityException("Access denied");
        
        // Act
        ModelAndView result = exceptionHandler.handleSecurityException(exception, request);
        
        // Assert
        assertEquals("error/error", result.getViewName());
        assertEquals("Access denied. You do not have permission to access this resource.", result.getModel().get("error"));
        assertEquals("ACCESS_DENIED", result.getModel().get("errorCode"));
    }
    
    @Test
    void testHandleGenericException() {
        // Arrange
        RuntimeException exception = new RuntimeException("Unexpected error");
        
        // Act
        ModelAndView result = exceptionHandler.handleGenericException(exception, request);
        
        // Assert
        assertEquals("error/error", result.getViewName());
        assertEquals("An unexpected error occurred. Please try again later.", result.getModel().get("error"));
        assertEquals("INTERNAL_ERROR", result.getModel().get("errorCode"));
        assertNotNull(result.getModel().get("timestamp"));
        assertEquals("/test/path", result.getModel().get("path"));
    }
}