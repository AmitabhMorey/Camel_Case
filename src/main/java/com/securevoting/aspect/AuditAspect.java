package com.securevoting.aspect;

import com.securevoting.model.AuditEventType;
import com.securevoting.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Aspect for automatic audit logging of system activities.
 */
@Aspect
@Component
public class AuditAspect {
    
    @Autowired
    private AuditService auditService;
    
    /**
     * Log successful authentication attempts.
     */
    @AfterReturning(pointcut = "execution(* com.securevoting.service.AuthenticationService.authenticate*(..))", 
                   returning = "result")
    public void logAuthenticationSuccess(JoinPoint joinPoint, Object result) {
        try {
            String methodName = joinPoint.getSignature().getName();
            Object[] args = joinPoint.getArgs();
            String userId = args.length > 0 ? (String) args[0] : "UNKNOWN";
            String ipAddress = getClientIpAddress();
            
            String method = extractAuthenticationMethod(methodName);
            
            auditService.logAuthenticationAttempt(userId, true, method, ipAddress);
        } catch (Exception e) {
            // Don't let audit logging failures affect the main operation
            System.err.println("Failed to log authentication success: " + e.getMessage());
        }
    }
    
    /**
     * Log failed authentication attempts.
     */
    @AfterThrowing(pointcut = "execution(* com.securevoting.service.AuthenticationService.authenticate*(..))", 
                  throwing = "exception")
    public void logAuthenticationFailure(JoinPoint joinPoint, Exception exception) {
        try {
            String methodName = joinPoint.getSignature().getName();
            Object[] args = joinPoint.getArgs();
            String userId = args.length > 0 ? (String) args[0] : "UNKNOWN";
            String ipAddress = getClientIpAddress();
            
            String method = extractAuthenticationMethod(methodName);
            
            auditService.logAuthenticationAttempt(userId, false, method, ipAddress);
        } catch (Exception e) {
            // Don't let audit logging failures affect the main operation
            System.err.println("Failed to log authentication failure: " + e.getMessage());
        }
    }
    
    /**
     * Log successful vote casting.
     */
    @AfterReturning(pointcut = "execution(* com.securevoting.service.VotingService.castVote(..))", 
                   returning = "result")
    public void logVoteCastSuccess(JoinPoint joinPoint, Object result) {
        try {
            Object[] args = joinPoint.getArgs();
            String userId = args.length > 0 ? (String) args[0] : "UNKNOWN";
            String electionId = args.length > 2 ? (String) args[2] : "UNKNOWN";
            String ipAddress = getClientIpAddress();
            
            auditService.logVotingEvent(userId, electionId, AuditEventType.VOTE_SUCCESS, ipAddress);
        } catch (Exception e) {
            System.err.println("Failed to log vote cast success: " + e.getMessage());
        }
    }
    
    /**
     * Log failed vote casting attempts.
     */
    @AfterThrowing(pointcut = "execution(* com.securevoting.service.VotingService.castVote(..))", 
                  throwing = "exception")
    public void logVoteCastFailure(JoinPoint joinPoint, Exception exception) {
        try {
            Object[] args = joinPoint.getArgs();
            String userId = args.length > 0 ? (String) args[0] : "UNKNOWN";
            String electionId = args.length > 2 ? (String) args[2] : "UNKNOWN";
            String ipAddress = getClientIpAddress();
            
            auditService.logVotingEvent(userId, electionId, AuditEventType.VOTE_FAILED, ipAddress);
        } catch (Exception e) {
            System.err.println("Failed to log vote cast failure: " + e.getMessage());
        }
    }
    
    /**
     * Log admin actions - disabled for now until AdminService is implemented.
     */
    // @AfterReturning(pointcut = "execution(* com.securevoting.service.AdminService.*(..))")
    public void logAdminAction(JoinPoint joinPoint) {
        // Implementation will be enabled when AdminService is created
    }
    
    /**
     * Log security events for suspicious activities.
     */
    @AfterThrowing(pointcut = "execution(* com.securevoting.service.*.*(..)) && " +
                             "@annotation(org.springframework.security.access.prepost.PreAuthorize)", 
                  throwing = "exception")
    public void logSecurityViolation(JoinPoint joinPoint, Exception exception) {
        try {
            String methodName = joinPoint.getSignature().getName();
            String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
            String ipAddress = getClientIpAddress();
            
            String details = String.format("Security violation in %s.%s: %s", 
                    className, methodName, exception.getMessage());
            
            auditService.logSecurityEvent(AuditEventType.SECURITY_VIOLATION, details, ipAddress);
        } catch (Exception e) {
            System.err.println("Failed to log security violation: " + e.getMessage());
        }
    }
    
    /**
     * Extract authentication method from method name.
     */
    private String extractAuthenticationMethod(String methodName) {
        if (methodName.contains("QR")) {
            return "QR_CODE";
        } else if (methodName.contains("OTP")) {
            return "OTP";
        } else {
            return "UNKNOWN";
        }
    }
    
    /**
     * Get current authenticated user ID.
     */
    private String getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                return authentication.getName();
            }
        } catch (Exception e) {
            // Ignore and return SYSTEM
        }
        return "SYSTEM";
    }
    
    /**
     * Get client IP address from request.
     */
    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes = 
                    (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();
            
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
            
            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return xRealIp;
            }
            
            return request.getRemoteAddr();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
    
    /**
     * Extract target information from method arguments.
     */
    private String extractTargetFromArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "UNKNOWN";
        }
        
        // Try to find an ID-like argument
        for (Object arg : args) {
            if (arg instanceof String) {
                String str = (String) arg;
                if (str.contains("-") || str.matches("\\d+")) {
                    return str;
                }
            }
        }
        
        // Return the first argument as string
        return args[0].toString();
    }
}