package com.securevoting.security;

import com.securevoting.model.AuditEventType;
import com.securevoting.service.AuditService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

/**
 * Security event detection filter that monitors for suspicious activities
 * and potential security threats in HTTP requests.
 */
@Component
public class SecurityEventDetectionFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(SecurityEventDetectionFilter.class);

    @Autowired
    private AuditService auditService;

    @Value("${security.detection.failed-attempts-threshold:5}")
    private int failedAttemptsThreshold;

    @Value("${security.detection.suspicious-patterns-enabled:true}")
    private boolean suspiciousPatternsEnabled;

    // Patterns for detecting common attack attempts
    private static final List<Pattern> SUSPICIOUS_PATTERNS = Arrays.asList(
        Pattern.compile("(?i).*(union|select|insert|delete|drop|create|alter|exec|script).*"),
        Pattern.compile("(?i).*(javascript:|vbscript:|onload|onerror|onclick).*"),
        Pattern.compile("(?i).*(<script|</script|<iframe|</iframe).*"),
        Pattern.compile("(?i).*(\\.\\.[\\/\\\\]|\\.\\.%2f|\\.\\.%5c).*"),
        Pattern.compile("(?i).*(cmd\\.exe|/bin/sh|/bin/bash|powershell).*")
    );

    // Track failed attempts per IP
    private final ConcurrentMap<String, FailedAttemptTracker> failedAttemptsMap = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String clientIp = getClientIpAddress(request);
        
        try {
            // Check for suspicious patterns in request
            if (suspiciousPatternsEnabled && containsSuspiciousPatterns(request)) {
                logger.warn("Suspicious request pattern detected from IP: {}", clientIp);
                auditService.logSecurityEvent(AuditEventType.SECURITY_VIOLATION, 
                    String.format("Suspicious request pattern detected: %s %s", 
                        request.getMethod(), request.getRequestURI()), 
                    clientIp);
            }

            // Check for excessive failed login attempts
            if (isLoginRequest(request) && response.getStatus() >= 400) {
                trackFailedAttempt(clientIp);
            } else if (isSuccessfulLogin(request, response)) {
                resetFailedAttempts(clientIp);
            }

            filterChain.doFilter(request, response);
            
        } catch (Exception e) {
            logger.error("Error in security event detection filter", e);
            filterChain.doFilter(request, response);
        }
    }

    private boolean containsSuspiciousPatterns(HttpServletRequest request) {
        // Check URL parameters
        String queryString = request.getQueryString();
        if (queryString != null && matchesSuspiciousPattern(queryString)) {
            return true;
        }

        // Check headers for suspicious content
        java.util.Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            if (headerValue != null && matchesSuspiciousPattern(headerValue)) {
                return true;
            }
        }

        // Check User-Agent for known attack tools
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && isKnownAttackTool(userAgent)) {
            return true;
        }

        return false;
    }

    private boolean matchesSuspiciousPattern(String input) {
        return SUSPICIOUS_PATTERNS.stream().anyMatch(pattern -> pattern.matcher(input).matches());
    }

    private boolean isKnownAttackTool(String userAgent) {
        String lowerUserAgent = userAgent.toLowerCase();
        return lowerUserAgent.contains("sqlmap") ||
               lowerUserAgent.contains("nikto") ||
               lowerUserAgent.contains("nessus") ||
               lowerUserAgent.contains("burp") ||
               lowerUserAgent.contains("zap") ||
               lowerUserAgent.contains("nmap") ||
               lowerUserAgent.isEmpty();
    }

    private boolean isLoginRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/login") || 
               path.equals("/qr-auth") || 
               path.equals("/otp-auth");
    }

    private boolean isSuccessfulLogin(HttpServletRequest request, HttpServletResponse response) {
        return isLoginRequest(request) && response.getStatus() < 400;
    }

    private void trackFailedAttempt(String clientIp) {
        FailedAttemptTracker tracker = failedAttemptsMap.computeIfAbsent(clientIp, 
            k -> new FailedAttemptTracker());
        
        synchronized (tracker) {
            tracker.incrementAttempts();
            
            if (tracker.getAttempts() >= failedAttemptsThreshold) {
                logger.warn("Multiple failed login attempts detected from IP: {} ({})", 
                    clientIp, tracker.getAttempts());
                
                auditService.logSecurityEvent(AuditEventType.SECURITY_VIOLATION, 
                    String.format("Multiple failed login attempts detected (%d attempts)", 
                        tracker.getAttempts()), 
                    clientIp);
                
                // Reset counter after logging to prevent spam
                tracker.reset();
            }
        }
    }

    private void resetFailedAttempts(String clientIp) {
        FailedAttemptTracker tracker = failedAttemptsMap.get(clientIp);
        if (tracker != null) {
            synchronized (tracker) {
                tracker.reset();
            }
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * Cleanup method to remove old entries and prevent memory leaks.
     */
    public void cleanupOldEntries() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);
        
        failedAttemptsMap.entrySet().removeIf(entry -> {
            FailedAttemptTracker tracker = entry.getValue();
            synchronized (tracker) {
                return tracker.getLastAttempt().isBefore(cutoff);
            }
        });
        
        logger.debug("Security event detection cleanup completed");
    }

    /**
     * Inner class to track failed attempts per IP address.
     */
    private static class FailedAttemptTracker {
        private int attempts = 0;
        private LocalDateTime lastAttempt = LocalDateTime.now();

        public void incrementAttempts() {
            this.attempts++;
            this.lastAttempt = LocalDateTime.now();
        }

        public void reset() {
            this.attempts = 0;
            this.lastAttempt = LocalDateTime.now();
        }

        public int getAttempts() {
            return attempts;
        }

        public LocalDateTime getLastAttempt() {
            return lastAttempt;
        }
    }
}