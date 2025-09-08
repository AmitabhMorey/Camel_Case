package com.securevoting.controller;

import com.securevoting.exception.CryptographyException;
import com.securevoting.exception.OTPGenerationException;
import com.securevoting.exception.QRCodeGenerationException;
import com.securevoting.exception.VotingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Global exception handler for the voting system.
 * Provides centralized error handling with proper error responses and logging.
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handle voting-related exceptions.
     */
    @ExceptionHandler(VotingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView handleVotingException(VotingException ex, HttpServletRequest request) {
        logger.error("Voting exception occurred: {} at {}", ex.getMessage(), request.getRequestURI(), ex);
        
        ModelAndView mav = new ModelAndView();
        mav.addObject("error", ex.getMessage());
        mav.addObject("errorCode", "VOTING_ERROR");
        mav.addObject("timestamp", LocalDateTime.now());
        mav.addObject("path", request.getRequestURI());
        
        // Redirect to appropriate page based on the request URI
        if (request.getRequestURI().contains("/voting/")) {
            mav.setViewName("redirect:/voting/elections");
        } else {
            mav.setViewName("error/error");
        }
        
        return mav;
    }
    
    /**
     * Handle cryptography-related exceptions.
     */
    @ExceptionHandler(CryptographyException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleCryptographyException(CryptographyException ex, HttpServletRequest request) {
        logger.error("Cryptography exception occurred: {} at {}", ex.getMessage(), request.getRequestURI(), ex);
        
        ModelAndView mav = new ModelAndView("error/error");
        mav.addObject("error", "A security error occurred. Please try again.");
        mav.addObject("errorCode", "SECURITY_ERROR");
        mav.addObject("timestamp", LocalDateTime.now());
        mav.addObject("path", request.getRequestURI());
        
        return mav;
    }
    
    /**
     * Handle QR code generation exceptions.
     */
    @ExceptionHandler(QRCodeGenerationException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleQRCodeException(QRCodeGenerationException ex, HttpServletRequest request) {
        logger.error("QR code generation exception occurred: {} at {}", ex.getMessage(), request.getRequestURI(), ex);
        
        ModelAndView mav = new ModelAndView();
        mav.addObject("error", "Failed to generate QR code. Please try again.");
        mav.addObject("errorCode", "QR_CODE_ERROR");
        mav.addObject("timestamp", LocalDateTime.now());
        mav.addObject("path", request.getRequestURI());
        
        // Redirect to login if QR code generation fails
        if (request.getRequestURI().contains("/qr-auth")) {
            mav.setViewName("redirect:/login?error=qr");
        } else {
            mav.setViewName("error/error");
        }
        
        return mav;
    }
    
    /**
     * Handle OTP generation exceptions.
     */
    @ExceptionHandler(OTPGenerationException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleOTPException(OTPGenerationException ex, HttpServletRequest request) {
        logger.error("OTP generation exception occurred: {} at {}", ex.getMessage(), request.getRequestURI(), ex);
        
        ModelAndView mav = new ModelAndView();
        mav.addObject("error", "Failed to generate OTP. Please try again.");
        mav.addObject("errorCode", "OTP_ERROR");
        mav.addObject("timestamp", LocalDateTime.now());
        mav.addObject("path", request.getRequestURI());
        
        // Redirect to QR auth if OTP generation fails
        if (request.getRequestURI().contains("/otp-auth")) {
            mav.setViewName("redirect:/qr-auth?error=otp");
        } else {
            mav.setViewName("error/error");
        }
        
        return mav;
    }
    
    /**
     * Handle validation exceptions from @Valid annotations.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        logger.warn("Validation exception occurred at {}: {}", request.getRequestURI(), ex.getMessage());
        
        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });
        
        ModelAndView mav = new ModelAndView();
        mav.addObject("validationErrors", validationErrors);
        mav.addObject("error", "Please correct the validation errors and try again.");
        mav.addObject("errorCode", "VALIDATION_ERROR");
        mav.addObject("timestamp", LocalDateTime.now());
        mav.addObject("path", request.getRequestURI());
        
        // Return to the form page with errors
        if (request.getRequestURI().contains("/register")) {
            mav.setViewName("register");
        } else if (request.getRequestURI().contains("/voting/")) {
            mav.setViewName("redirect:/voting/elections");
        } else {
            mav.setViewName("error/error");
        }
        
        return mav;
    }
    
    /**
     * Handle bind exceptions (form binding errors).
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView handleBindException(BindException ex, HttpServletRequest request) {
        logger.warn("Bind exception occurred at {}: {}", request.getRequestURI(), ex.getMessage());
        
        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });
        
        ModelAndView mav = new ModelAndView();
        mav.addObject("validationErrors", validationErrors);
        mav.addObject("error", "Please correct the form errors and try again.");
        mav.addObject("errorCode", "FORM_ERROR");
        mav.addObject("timestamp", LocalDateTime.now());
        mav.addObject("path", request.getRequestURI());
        
        // Return to the appropriate form page
        if (request.getRequestURI().contains("/register")) {
            mav.setViewName("register");
        } else {
            mav.setViewName("error/error");
        }
        
        return mav;
    }
    
    /**
     * Handle constraint violation exceptions (parameter validation).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView handleConstraintViolationException(ConstraintViolationException ex, HttpServletRequest request) {
        logger.warn("Constraint violation exception occurred at {}: {}", request.getRequestURI(), ex.getMessage());
        
        Map<String, String> validationErrors = new HashMap<>();
        Set<ConstraintViolation<?>> violations = ex.getConstraintViolations();
        
        for (ConstraintViolation<?> violation : violations) {
            String fieldName = violation.getPropertyPath().toString();
            String errorMessage = violation.getMessage();
            validationErrors.put(fieldName, errorMessage);
        }
        
        ModelAndView mav = new ModelAndView();
        mav.addObject("validationErrors", validationErrors);
        mav.addObject("error", "Invalid input parameters. Please check your input and try again.");
        mav.addObject("errorCode", "PARAMETER_ERROR");
        mav.addObject("timestamp", LocalDateTime.now());
        mav.addObject("path", request.getRequestURI());
        
        // Redirect to appropriate page
        if (request.getRequestURI().contains("/voting/")) {
            mav.setViewName("redirect:/voting/elections");
        } else {
            mav.setViewName("error/error");
        }
        
        return mav;
    }
    
    /**
     * Handle method argument type mismatch exceptions.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView handleTypeMismatchException(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        logger.warn("Type mismatch exception occurred at {}: {}", request.getRequestURI(), ex.getMessage());
        
        ModelAndView mav = new ModelAndView();
        mav.addObject("error", "Invalid parameter format: " + ex.getName());
        mav.addObject("errorCode", "TYPE_MISMATCH");
        mav.addObject("timestamp", LocalDateTime.now());
        mav.addObject("path", request.getRequestURI());
        
        // Redirect to appropriate page
        if (request.getRequestURI().contains("/voting/")) {
            mav.setViewName("redirect:/voting/elections");
        } else {
            mav.setViewName("error/error");
        }
        
        return mav;
    }
    
    /**
     * Handle illegal argument exceptions.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        logger.warn("Illegal argument exception occurred at {}: {}", request.getRequestURI(), ex.getMessage());
        
        ModelAndView mav = new ModelAndView();
        mav.addObject("error", ex.getMessage());
        mav.addObject("errorCode", "INVALID_ARGUMENT");
        mav.addObject("timestamp", LocalDateTime.now());
        mav.addObject("path", request.getRequestURI());
        
        // Redirect to appropriate page based on context
        if (request.getRequestURI().contains("/register")) {
            mav.setViewName("register");
        } else if (request.getRequestURI().contains("/voting/")) {
            mav.setViewName("redirect:/voting/elections");
        } else {
            mav.setViewName("error/error");
        }
        
        return mav;
    }
    
    /**
     * Handle security exceptions (authentication/authorization).
     */
    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ModelAndView handleSecurityException(SecurityException ex, HttpServletRequest request) {
        logger.error("Security exception occurred at {}: {}", request.getRequestURI(), ex.getMessage());
        
        ModelAndView mav = new ModelAndView("error/error");
        mav.addObject("error", "Access denied. You do not have permission to access this resource.");
        mav.addObject("errorCode", "ACCESS_DENIED");
        mav.addObject("timestamp", LocalDateTime.now());
        mav.addObject("path", request.getRequestURI());
        
        return mav;
    }
    
    /**
     * Handle all other unexpected exceptions.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleGenericException(Exception ex, HttpServletRequest request) {
        logger.error("Unexpected exception occurred at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        
        ModelAndView mav = new ModelAndView("error/error");
        mav.addObject("error", "An unexpected error occurred. Please try again later.");
        mav.addObject("errorCode", "INTERNAL_ERROR");
        mav.addObject("timestamp", LocalDateTime.now());
        mav.addObject("path", request.getRequestURI());
        
        return mav;
    }
}