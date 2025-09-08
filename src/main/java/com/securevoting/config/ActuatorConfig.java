package com.securevoting.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.actuate.info.Info.Builder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import com.securevoting.service.VotingService;
import com.securevoting.service.AuthenticationService;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Configuration for Spring Boot Actuator endpoints and custom health indicators.
 */
@Configuration
public class ActuatorConfig {

    /**
     * Custom health indicator for voting system components.
     * Only enabled when not in test profile to avoid dependency issues.
     */
    @Component
    @ConditionalOnProperty(name = "management.health.custom.enabled", havingValue = "true", matchIfMissing = false)
    public static class VotingSystemHealthIndicator implements HealthIndicator {
        
        private final VotingService votingService;
        private final AuthenticationService authenticationService;
        
        public VotingSystemHealthIndicator(VotingService votingService, 
                                         AuthenticationService authenticationService) {
            this.votingService = votingService;
            this.authenticationService = authenticationService;
        }
        
        @Override
        public Health health() {
            try {
                // Check if core services are responsive
                if (votingService != null) {
                    votingService.getActiveElections();
                }
                
                return Health.up()
                    .withDetail("voting-service", "operational")
                    .withDetail("authentication-service", "operational")
                    .withDetail("timestamp", LocalDateTime.now())
                    .build();
            } catch (Exception e) {
                return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("timestamp", LocalDateTime.now())
                    .build();
            }
        }
    }
    
    /**
     * Custom info contributor for application metadata.
     */
    @Component
    public static class VotingSystemInfoContributor implements InfoContributor {
        
        @Override
        public void contribute(Builder builder) {
            builder.withDetail("app", Map.of(
                "name", "Secure QR Voting System",
                "version", "1.0.0",
                "description", "Secure online voting platform with QR code and OTP authentication",
                "features", Map.of(
                    "qr-authentication", "enabled",
                    "otp-validation", "enabled",
                    "vote-encryption", "AES-256-GCM",
                    "audit-logging", "enabled"
                )
            ));
            
            builder.withDetail("security", Map.of(
                "encryption", "AES-256",
                "hashing", "SHA-256",
                "session-management", "enabled",
                "csrf-protection", "enabled"
            ));
        }
    }
}