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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Rate limiting filter to prevent abuse of authentication and voting endpoints.
 * Implements sliding window rate limiting with configurable limits per IP address.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);

    @Autowired
    private AuditService auditService;

    @Value("${security.rate-limit.auth.requests-per-minute:10}")
    private int authRequestsPerMinute;

    @Value("${security.rate-limit.voting.requests-per-minute:5}")
    private int votingRequestsPerMinute;

    @Value("${security.rate-limit.general.requests-per-minute:60}")
    private int generalRequestsPerMinute;

    // In-memory storage for rate limiting (in production, use Redis)
    private final ConcurrentMap<String, RequestCounter> authRateLimitMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, RequestCounter> votingRateLimitMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, RequestCounter> generalRateLimitMap = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String clientIp = getClientIpAddress(request);
        String requestPath = request.getRequestURI();
        
        try {
            if (isRateLimited(clientIp, requestPath)) {
                logger.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, requestPath);
                
                // Log security event
                auditService.logSecurityEvent(AuditEventType.SECURITY_VIOLATION, 
                    String.format("Rate limit exceeded for IP: %s on path: %s", clientIp, requestPath), 
                    clientIp);
                
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Rate limit exceeded. Please try again later.\"}");
                return;
            }
            
            filterChain.doFilter(request, response);
            
        } catch (Exception e) {
            logger.error("Error in rate limiting filter", e);
            filterChain.doFilter(request, response);
        }
    }

    private boolean isRateLimited(String clientIp, String requestPath) {
        LocalDateTime now = LocalDateTime.now();
        
        if (isAuthenticationPath(requestPath)) {
            return checkRateLimit(authRateLimitMap, clientIp, now, authRequestsPerMinute);
        } else if (isVotingPath(requestPath)) {
            return checkRateLimit(votingRateLimitMap, clientIp, now, votingRequestsPerMinute);
        } else {
            return checkRateLimit(generalRateLimitMap, clientIp, now, generalRequestsPerMinute);
        }
    }

    private boolean checkRateLimit(ConcurrentMap<String, RequestCounter> rateLimitMap, 
                                 String clientIp, LocalDateTime now, int maxRequests) {
        
        RequestCounter counter = rateLimitMap.computeIfAbsent(clientIp, k -> new RequestCounter());
        
        synchronized (counter) {
            // Clean up old requests (older than 1 minute)
            counter.getRequestTimes().removeIf(time -> time.isBefore(now.minusMinutes(1)));
            
            // Check if limit exceeded
            if (counter.getRequestTimes().size() >= maxRequests) {
                return true; // Rate limited
            }
            
            // Add current request
            counter.getRequestTimes().add(now);
            return false; // Not rate limited
        }
    }

    private boolean isAuthenticationPath(String path) {
        return path.startsWith("/login") || 
               path.startsWith("/register") || 
               path.startsWith("/qr-auth") || 
               path.startsWith("/otp-auth");
    }

    private boolean isVotingPath(String path) {
        return path.startsWith("/voting/") || 
               path.startsWith("/vote");
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
     * Should be called periodically by a scheduled task.
     */
    public void cleanupOldEntries() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(5);
        
        cleanupMap(authRateLimitMap, cutoff);
        cleanupMap(votingRateLimitMap, cutoff);
        cleanupMap(generalRateLimitMap, cutoff);
        
        logger.debug("Rate limiting cleanup completed");
    }

    private void cleanupMap(ConcurrentMap<String, RequestCounter> map, LocalDateTime cutoff) {
        map.entrySet().removeIf(entry -> {
            RequestCounter counter = entry.getValue();
            synchronized (counter) {
                counter.getRequestTimes().removeIf(time -> time.isBefore(cutoff));
                return counter.getRequestTimes().isEmpty();
            }
        });
    }

    /**
     * Inner class to track request counts per IP address.
     */
    private static class RequestCounter {
        private final java.util.List<LocalDateTime> requestTimes = new java.util.ArrayList<>();

        public java.util.List<LocalDateTime> getRequestTimes() {
            return requestTimes;
        }
    }
}