package com.securevoting.controller;

import com.securevoting.model.User;
import com.securevoting.security.InputSanitizer;
import com.securevoting.security.MultiFactorAuthenticationToken;
import com.securevoting.service.AuthenticationResult;
import com.securevoting.service.AuthenticationService;
import com.securevoting.service.QRCodeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller handling user authentication flows including registration,
 * QR code authentication, and OTP validation.
 */
@Controller
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    @Autowired
    private AuthenticationService authenticationService;
    
    @Autowired
    private QRCodeService qrCodeService;
    
    @Autowired
    private InputSanitizer inputSanitizer;
    
    /**
     * Display login page.
     */
    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                           @RequestParam(value = "logout", required = false) String logout,
                           Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid username or password");
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully");
        }
        return "login";
    }
    
    /**
     * Display registration page.
     */
    @GetMapping("/register")
    public String registrationPage(Model model) {
        model.addAttribute("user", new UserRegistrationDto());
        return "register";
    }
    
    /**
     * Handle user registration.
     */
    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") UserRegistrationDto userDto,
                              BindingResult result,
                              RedirectAttributes redirectAttributes) {
        
        // Check for validation errors
        if (result.hasErrors()) {
            return "register";
        }
        
        // Additional security validation
        if (!inputSanitizer.isValidUsername(userDto.getUsername())) {
            result.rejectValue("username", "error.username.invalid", 
                "Username must be 3-20 characters and contain only letters, numbers, underscores, and hyphens");
            return "register";
        }
        
        if (!inputSanitizer.isValidEmail(userDto.getEmail())) {
            result.rejectValue("email", "error.email.invalid", 
                "Please enter a valid email address");
            return "register";
        }
        
        // Custom validation for password matching
        if (!userDto.isPasswordMatching()) {
            result.rejectValue("confirmPassword", "error.password.mismatch", 
                "Password and confirmation password do not match");
            return "register";
        }
        
        try {
            // Sanitize inputs before processing
            String sanitizedUsername = inputSanitizer.sanitizeUserInput(userDto.getUsername());
            String sanitizedEmail = inputSanitizer.sanitizeUserInput(userDto.getEmail());
            
            User user = authenticationService.registerUser(
                sanitizedUsername, 
                sanitizedEmail, 
                userDto.getPassword()
            );
            
            logger.info("User registered successfully: {}", user.getUsername());
            redirectAttributes.addFlashAttribute("message", 
                "Registration successful! Please login to continue.");
            return "redirect:/login";
            
        } catch (IllegalArgumentException e) {
            result.rejectValue("username", "error.user", e.getMessage());
            return "register";
        } catch (Exception e) {
            logger.error("Registration failed for user: {}", userDto.getUsername(), e);
            result.rejectValue("username", "error.user", "Registration failed. Please try again.");
            return "register";
        }
    }
    
    /**
     * Display QR code authentication page after successful first factor.
     */
    @GetMapping("/qr-auth")
    public String qrAuthPage(Model model, HttpSession session) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth instanceof MultiFactorAuthenticationToken mfaToken) {
            if (mfaToken.isFirstFactorComplete() && !mfaToken.isSecondFactorComplete()) {
                // Generate QR code for the user
                try {
                    String qrData = qrCodeService.generateQRCodeData(mfaToken.getUserId());
                    byte[] qrCodeImage = qrCodeService.generateQRCodeImage(qrData);
                    
                    model.addAttribute("qrCodeData", qrData);
                    model.addAttribute("qrCodeImage", java.util.Base64.getEncoder().encodeToString(qrCodeImage));
                    model.addAttribute("userId", mfaToken.getUserId());
                    
                    return "qr-auth";
                } catch (Exception e) {
                    logger.error("Failed to generate QR code for user: {}", mfaToken.getUserId(), e);
                    model.addAttribute("error", "Failed to generate QR code. Please try again.");
                    return "qr-auth";
                }
            }
        }
        
        // Redirect to login if not in proper MFA state
        return "redirect:/login";
    }
    
    /**
     * Handle QR code validation and proceed to OTP.
     */
    @PostMapping("/qr-auth")
    public String validateQRCode(@RequestParam("qrData") @NotBlank(message = "QR code data is required") String qrData,
                                @RequestParam("userId") @NotBlank(message = "User ID is required") String userId,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        
        try {
            AuthenticationResult result = authenticationService.authenticateWithQR(userId, qrData);
            
            if (result.isSuccessful()) {
                // QR validation successful, proceed to OTP
                redirectAttributes.addFlashAttribute("message", "QR code validated. Please enter your OTP.");
                redirectAttributes.addFlashAttribute("userId", userId);
                return "redirect:/otp-auth";
            } else {
                model.addAttribute("error", result.getErrorMessage());
                model.addAttribute("userId", userId);
                return "qr-auth";
            }
            
        } catch (Exception e) {
            logger.error("QR validation failed for user: {}", userId, e);
            model.addAttribute("error", "QR code validation failed. Please try again.");
            model.addAttribute("userId", userId);
            return "qr-auth";
        }
    }
    
    /**
     * Display OTP authentication page.
     */
    @GetMapping("/otp-auth")
    public String otpAuthPage(@ModelAttribute("userId") String userId, Model model) {
        if (userId == null || userId.isEmpty()) {
            return "redirect:/login";
        }
        
        model.addAttribute("userId", userId);
        return "otp-auth";
    }
    
    /**
     * Handle OTP validation and complete authentication.
     */
    @PostMapping("/otp-auth")
    public String validateOTP(@RequestParam("otp") @NotBlank(message = "OTP is required") 
                             @Pattern(regexp = "^\\d{6}$", message = "OTP must be 6 digits") String otp,
                             @RequestParam("userId") @NotBlank(message = "User ID is required") String userId,
                             Model model,
                             HttpServletRequest request) {
        
        try {
            AuthenticationResult result = authenticationService.authenticateWithOTP(userId, otp);
            
            if (result.isSuccessful()) {
                // Create authenticated session
                HttpSession session = request.getSession(true);
                session.setAttribute("userId", userId);
                session.setAttribute("sessionId", result.getSessionId());
                session.setAttribute("authenticated", true);
                
                logger.info("User {} successfully authenticated with MFA", userId);
                return "redirect:/dashboard";
            } else {
                model.addAttribute("error", result.getErrorMessage());
                model.addAttribute("userId", userId);
                return "otp-auth";
            }
            
        } catch (Exception e) {
            logger.error("OTP validation failed for user: {}", userId, e);
            model.addAttribute("error", "OTP validation failed. Please try again.");
            model.addAttribute("userId", userId);
            return "otp-auth";
        }
    }
    
    /**
     * DTO for user registration form with validation annotations.
     */
    public static class UserRegistrationDto {
        
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
        private String username;
        
        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email address")
        @Size(max = 100, message = "Email cannot exceed 100 characters")
        private String email;
        
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$", 
                message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character")
        private String password;
        
        @NotBlank(message = "Password confirmation is required")
        private String confirmPassword;
        
        // Getters and setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        
        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
        
        /**
         * Custom validation to check if password and confirmPassword match.
         */
        public boolean isPasswordMatching() {
            return password != null && password.equals(confirmPassword);
        }
    }
}