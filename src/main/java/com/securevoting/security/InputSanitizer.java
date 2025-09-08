package com.securevoting.security;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.util.regex.Pattern;

/**
 * Utility class for input sanitization and XSS prevention.
 * Provides methods to clean and validate user input.
 */
@Component
public class InputSanitizer {

    // Patterns for detecting potentially malicious input
    private static final Pattern SCRIPT_PATTERN = Pattern.compile(
        "(?i)<script[^>]*>.*?</script>|javascript:|vbscript:|onload|onerror|onclick|onmouseover"
    );
    
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i).*(union\\s|select\\s|insert\\s|delete\\s|drop\\s|create\\s|alter\\s|exec\\s|declare\\s|cast\\s|'\\s*or\\s*'|'\\s*and\\s*').*"
    );
    
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(
        ".*(\\.\\./|\\.\\.\\\\|%2e%2e%2f|%2e%2e%5c).*"
    );

    /**
     * Sanitizes HTML content by escaping potentially dangerous characters.
     * 
     * @param input the input string to sanitize
     * @return sanitized string safe for HTML output
     */
    public String sanitizeHtml(String input) {
        if (input == null) {
            return null;
        }
        
        // Use Spring's HtmlUtils for basic HTML escaping
        String sanitized = HtmlUtils.htmlEscape(input);
        
        // Additional cleaning for script tags and event handlers
        sanitized = SCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        
        return sanitized.trim();
    }

    /**
     * Validates input for SQL injection patterns.
     * 
     * @param input the input string to validate
     * @return true if input appears safe, false if suspicious patterns detected
     */
    public boolean isSqlSafe(String input) {
        if (input == null) {
            return true;
        }
        
        return !SQL_INJECTION_PATTERN.matcher(input).matches();
    }

    /**
     * Validates input for path traversal patterns.
     * 
     * @param input the input string to validate
     * @return true if input appears safe, false if path traversal detected
     */
    public boolean isPathSafe(String input) {
        if (input == null) {
            return true;
        }
        
        return !PATH_TRAVERSAL_PATTERN.matcher(input).matches();
    }

    /**
     * Sanitizes user input for safe storage and display.
     * Combines HTML escaping with additional security checks.
     * 
     * @param input the input string to sanitize
     * @return sanitized string
     * @throws SecurityException if input contains dangerous patterns
     */
    public String sanitizeUserInput(String input) {
        if (input == null) {
            return null;
        }

        // Check for dangerous patterns
        if (!isSqlSafe(input)) {
            throw new SecurityException("Input contains potentially dangerous SQL patterns");
        }
        
        if (!isPathSafe(input)) {
            throw new SecurityException("Input contains path traversal patterns");
        }

        // Sanitize HTML content
        String sanitized = sanitizeHtml(input);
        
        // Limit length to prevent DoS attacks
        if (sanitized.length() > 1000) {
            sanitized = sanitized.substring(0, 1000);
        }
        
        return sanitized;
    }

    /**
     * Validates and sanitizes election-related input (titles, descriptions, etc.).
     * 
     * @param input the input string to sanitize
     * @return sanitized string safe for election data
     */
    public String sanitizeElectionInput(String input) {
        if (input == null) {
            return null;
        }

        // More restrictive sanitization for election data
        String sanitized = sanitizeUserInput(input);
        
        // Remove any remaining HTML tags
        sanitized = sanitized.replaceAll("<[^>]*>", "");
        
        // Normalize whitespace
        sanitized = sanitized.replaceAll("\\s+", " ").trim();
        
        return sanitized;
    }

    /**
     * Validates username format for security.
     * 
     * @param username the username to validate
     * @return true if username format is valid and safe
     */
    public boolean isValidUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        
        // Username should only contain alphanumeric characters, underscores, and hyphens
        Pattern usernamePattern = Pattern.compile("^[a-zA-Z0-9_-]{3,20}$");
        return usernamePattern.matcher(username.trim()).matches();
    }

    /**
     * Validates email format for security.
     * 
     * @param email the email to validate
     * @return true if email format is valid and safe
     */
    public boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        // Basic email validation pattern
        Pattern emailPattern = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
        );
        
        String trimmedEmail = email.trim();
        return trimmedEmail.length() <= 100 && emailPattern.matcher(trimmedEmail).matches();
    }
}