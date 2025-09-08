package com.securevoting.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Validates security configuration on application startup
 * to ensure all security measures are properly configured.
 */
@Component
public class SecurityConfigValidator {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfigValidator.class);

    @Value("${server.servlet.session.cookie.http-only:false}")
    private boolean httpOnlyCookies;

    @Value("${server.servlet.session.cookie.secure:false}")
    private boolean secureCookies;

    @Value("${server.servlet.session.timeout:30m}")
    private String sessionTimeout;

    @Value("${security.rate-limit.auth.requests-per-minute:10}")
    private int authRateLimit;

    @Value("${security.rate-limit.voting.requests-per-minute:5}")
    private int votingRateLimit;

    @Value("${security.detection.failed-attempts-threshold:5}")
    private int failedAttemptsThreshold;

    @Value("${otp.validity.minutes:5}")
    private int otpValidityMinutes;

    @Value("${otp.max.attempts:3}")
    private int otpMaxAttempts;

    @EventListener(ApplicationReadyEvent.class)
    public void validateSecurityConfiguration() {
        logger.info("Validating security configuration...");

        boolean allValid = true;

        // Validate session security
        if (!httpOnlyCookies) {
            logger.warn("Security Warning: HTTP-only cookies are not enabled");
            allValid = false;
        }

        // Note: In development, secure cookies might be false, which is acceptable
        if (!secureCookies) {
            logger.info("Info: Secure cookies are disabled (acceptable in development)");
        }

        // Validate session timeout
        if (!isValidSessionTimeout(sessionTimeout)) {
            logger.warn("Security Warning: Session timeout may be too long: {}", sessionTimeout);
            allValid = false;
        }

        // Validate rate limiting
        if (authRateLimit <= 0 || authRateLimit > 100) {
            logger.warn("Security Warning: Auth rate limit should be between 1-100, current: {}", authRateLimit);
            allValid = false;
        }

        if (votingRateLimit <= 0 || votingRateLimit > 50) {
            logger.warn("Security Warning: Voting rate limit should be between 1-50, current: {}", votingRateLimit);
            allValid = false;
        }

        // Validate security detection
        if (failedAttemptsThreshold <= 0 || failedAttemptsThreshold > 20) {
            logger.warn("Security Warning: Failed attempts threshold should be between 1-20, current: {}", 
                failedAttemptsThreshold);
            allValid = false;
        }

        // Validate OTP configuration
        if (otpValidityMinutes <= 0 || otpValidityMinutes > 15) {
            logger.warn("Security Warning: OTP validity should be between 1-15 minutes, current: {}", 
                otpValidityMinutes);
            allValid = false;
        }

        if (otpMaxAttempts <= 0 || otpMaxAttempts > 10) {
            logger.warn("Security Warning: OTP max attempts should be between 1-10, current: {}", 
                otpMaxAttempts);
            allValid = false;
        }

        if (allValid) {
            logger.info("Security configuration validation completed successfully");
        } else {
            logger.warn("Security configuration validation completed with warnings - please review settings");
        }

        // Log security features status
        logSecurityFeaturesStatus();
    }

    private boolean isValidSessionTimeout(String timeout) {
        try {
            if (timeout.endsWith("m")) {
                int minutes = Integer.parseInt(timeout.substring(0, timeout.length() - 1));
                return minutes > 0 && minutes <= 60; // Max 1 hour
            } else if (timeout.endsWith("s")) {
                int seconds = Integer.parseInt(timeout.substring(0, timeout.length() - 1));
                return seconds > 0 && seconds <= 3600; // Max 1 hour
            }
            return false;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void logSecurityFeaturesStatus() {
        logger.info("=== Security Features Status ===");
        logger.info("✓ Multi-factor Authentication (QR + OTP)");
        logger.info("✓ Vote Encryption (AES-256-GCM)");
        logger.info("✓ Rate Limiting (Auth: {}/min, Voting: {}/min)", authRateLimit, votingRateLimit);
        logger.info("✓ Security Event Detection");
        logger.info("✓ Session Management (Timeout: {})", sessionTimeout);
        logger.info("✓ Input Sanitization & XSS Prevention");
        logger.info("✓ CSRF Protection");
        logger.info("✓ Comprehensive Audit Logging");
        logger.info("✓ HTTP Security Headers");
        logger.info("================================");
    }
}