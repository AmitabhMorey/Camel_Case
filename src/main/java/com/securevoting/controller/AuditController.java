package com.securevoting.controller;

import com.securevoting.model.AuditEventType;
import com.securevoting.model.AuditLog;
import com.securevoting.service.AuditFilter;
import com.securevoting.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller handling audit log display with filtering and pagination capabilities.
 * Provides comprehensive audit trail viewing for administrators.
 */
@Controller
@RequestMapping("/admin/audit")
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuditController.class);
    
    @Autowired
    private AuditService auditService;
    
    /**
     * Display audit logs with filtering and pagination.
     */
    @GetMapping("/logs")
    public String viewAuditLogs(@RequestParam(value = "userId", required = false) String userId,
                               @RequestParam(value = "eventType", required = false) String eventTypeStr,
                               @RequestParam(value = "startDate", required = false) 
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                               @RequestParam(value = "endDate", required = false) 
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
                               @RequestParam(value = "action", required = false) String action,
                               @RequestParam(value = "ipAddress", required = false) String ipAddress,
                               @RequestParam(value = "page", defaultValue = "0") 
                               @Min(value = 0, message = "Page number must be non-negative") Integer page,
                               @RequestParam(value = "size", defaultValue = "50") 
                               @Min(value = 1, message = "Page size must be positive") 
                               @Max(value = 200, message = "Page size cannot exceed 200") Integer size,
                               Model model, 
                               HttpSession session, 
                               HttpServletRequest request) {
        
        String adminId = (String) session.getAttribute("userId");
        
        try {
            // Build audit filter from request parameters
            AuditFilter filter = new AuditFilter();
            filter.setUserId(userId);
            filter.setAction(action);
            filter.setIpAddress(ipAddress);
            filter.setStartDate(startDate);
            filter.setEndDate(endDate);
            filter.setLimit(size);
            filter.setOffset(page * size);
            
            // Parse event type if provided
            if (eventTypeStr != null && !eventTypeStr.isEmpty()) {
                try {
                    AuditEventType eventType = AuditEventType.valueOf(eventTypeStr.toUpperCase());
                    filter.setEventType(eventType);
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid event type provided: {}", eventTypeStr);
                    model.addAttribute("warning", "Invalid event type: " + eventTypeStr);
                }
            }
            
            // Convert filter to map for service call
            Map<String, Object> filterMap = convertFilterToMap(filter);
            
            // Retrieve audit logs
            List<AuditLog> auditLogs = auditService.getAuditLogs(filterMap);
            
            // Add model attributes
            model.addAttribute("auditLogs", auditLogs);
            model.addAttribute("filter", filter);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            model.addAttribute("eventTypes", AuditEventType.values());
            
            // Calculate pagination info
            boolean hasNextPage = auditLogs.size() == size; // Simple check - if we got full page, there might be more
            model.addAttribute("hasNextPage", hasNextPage);
            model.addAttribute("hasPreviousPage", page > 0);
            model.addAttribute("nextPage", page + 1);
            model.addAttribute("previousPage", Math.max(0, page - 1));
            
            // Log audit log access
            auditService.logAdminAction(adminId, "AUDIT_LOGS_ACCESSED", 
                "Audit logs viewed with filters: " + filter.toString(), 
                getClientIpAddress(request));
            
            return "admin/audit-logs";
            
        } catch (Exception e) {
            logger.error("Failed to load audit logs for admin: {}", adminId, e);
            model.addAttribute("error", "Failed to load audit logs. Please try again.");
            return "admin/audit-logs";
        }
    }
    
    /**
     * Display recent audit logs (last 24 hours).
     */
    @GetMapping("/recent")
    public String viewRecentAuditLogs(Model model, HttpSession session, HttpServletRequest request) {
        String adminId = (String) session.getAttribute("userId");
        
        try {
            AuditFilter filter = AuditFilter.recentLogs();
            Map<String, Object> filterMap = convertFilterToMap(filter);
            List<AuditLog> auditLogs = auditService.getAuditLogs(filterMap);
            
            model.addAttribute("auditLogs", auditLogs);
            model.addAttribute("filter", filter);
            model.addAttribute("isRecentView", true);
            
            // Log recent audit logs access
            auditService.logAdminAction(adminId, "RECENT_AUDIT_LOGS_ACCESSED", 
                "Recent audit logs viewed (last 24 hours)", 
                getClientIpAddress(request));
            
            return "admin/audit-logs";
            
        } catch (Exception e) {
            logger.error("Failed to load recent audit logs for admin: {}", adminId, e);
            model.addAttribute("error", "Failed to load recent audit logs. Please try again.");
            return "admin/audit-logs";
        }
    }
    
    /**
     * Display audit logs for a specific user.
     */
    @GetMapping("/user/{userId}")
    public String viewUserAuditLogs(@PathVariable("userId") String userId,
                                   @RequestParam(value = "startDate", required = false) 
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                                   @RequestParam(value = "endDate", required = false) 
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
                                   @RequestParam(value = "page", defaultValue = "0") 
                                   @Min(value = 0, message = "Page number must be non-negative") Integer page,
                                   @RequestParam(value = "size", defaultValue = "50") 
                                   @Min(value = 1, message = "Page size must be positive") 
                                   @Max(value = 200, message = "Page size cannot exceed 200") Integer size,
                                   Model model, 
                                   HttpSession session, 
                                   HttpServletRequest request) {
        
        String adminId = (String) session.getAttribute("userId");
        
        try {
            AuditFilter filter = AuditFilter.forUser(userId);
            filter.setStartDate(startDate);
            filter.setEndDate(endDate);
            filter.setLimit(size);
            filter.setOffset(page * size);
            
            Map<String, Object> filterMap = convertFilterToMap(filter);
            List<AuditLog> auditLogs = auditService.getAuditLogs(filterMap);
            
            model.addAttribute("auditLogs", auditLogs);
            model.addAttribute("filter", filter);
            model.addAttribute("targetUserId", userId);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            
            // Calculate pagination info
            boolean hasNextPage = auditLogs.size() == size;
            model.addAttribute("hasNextPage", hasNextPage);
            model.addAttribute("hasPreviousPage", page > 0);
            model.addAttribute("nextPage", page + 1);
            model.addAttribute("previousPage", Math.max(0, page - 1));
            
            // Log user-specific audit logs access
            auditService.logAdminAction(adminId, "USER_AUDIT_LOGS_ACCESSED", 
                "User audit logs viewed for user: " + userId, 
                getClientIpAddress(request));
            
            return "admin/user-audit-logs";
            
        } catch (Exception e) {
            logger.error("Failed to load audit logs for user {} by admin: {}", userId, adminId, e);
            model.addAttribute("error", "Failed to load user audit logs. Please try again.");
            return "admin/audit-logs";
        }
    }
    
    /**
     * Display audit logs by event type.
     */
    @GetMapping("/events/{eventType}")
    public String viewEventTypeAuditLogs(@PathVariable("eventType") String eventTypeStr,
                                        @RequestParam(value = "startDate", required = false) 
                                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                                        @RequestParam(value = "endDate", required = false) 
                                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
                                        @RequestParam(value = "page", defaultValue = "0") 
                                        @Min(value = 0, message = "Page number must be non-negative") Integer page,
                                        @RequestParam(value = "size", defaultValue = "50") 
                                        @Min(value = 1, message = "Page size must be positive") 
                                        @Max(value = 200, message = "Page size cannot exceed 200") Integer size,
                                        Model model, 
                                        HttpSession session, 
                                        HttpServletRequest request) {
        
        String adminId = (String) session.getAttribute("userId");
        
        try {
            // Parse event type
            AuditEventType eventType;
            try {
                eventType = AuditEventType.valueOf(eventTypeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid event type requested: {}", eventTypeStr);
                model.addAttribute("error", "Invalid event type: " + eventTypeStr);
                return "admin/audit-logs";
            }
            
            AuditFilter filter = AuditFilter.forEventType(eventType);
            filter.setStartDate(startDate);
            filter.setEndDate(endDate);
            filter.setLimit(size);
            filter.setOffset(page * size);
            
            Map<String, Object> filterMap = convertFilterToMap(filter);
            List<AuditLog> auditLogs = auditService.getAuditLogs(filterMap);
            
            model.addAttribute("auditLogs", auditLogs);
            model.addAttribute("filter", filter);
            model.addAttribute("targetEventType", eventType);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            
            // Calculate pagination info
            boolean hasNextPage = auditLogs.size() == size;
            model.addAttribute("hasNextPage", hasNextPage);
            model.addAttribute("hasPreviousPage", page > 0);
            model.addAttribute("nextPage", page + 1);
            model.addAttribute("previousPage", Math.max(0, page - 1));
            
            // Log event type audit logs access
            auditService.logAdminAction(adminId, "EVENT_TYPE_AUDIT_LOGS_ACCESSED", 
                "Event type audit logs viewed for: " + eventType, 
                getClientIpAddress(request));
            
            return "admin/event-audit-logs";
            
        } catch (Exception e) {
            logger.error("Failed to load audit logs for event type {} by admin: {}", eventTypeStr, adminId, e);
            model.addAttribute("error", "Failed to load event type audit logs. Please try again.");
            return "admin/audit-logs";
        }
    }
    
    /**
     * Export audit logs (placeholder for future CSV/PDF export functionality).
     */
    @GetMapping("/export")
    public String exportAuditLogs(@RequestParam(value = "format", defaultValue = "csv") String format,
                                 @RequestParam(value = "userId", required = false) String userId,
                                 @RequestParam(value = "eventType", required = false) String eventTypeStr,
                                 @RequestParam(value = "startDate", required = false) 
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                                 @RequestParam(value = "endDate", required = false) 
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
                                 Model model, 
                                 HttpSession session, 
                                 HttpServletRequest request) {
        
        String adminId = (String) session.getAttribute("userId");
        
        // Log export attempt
        auditService.logAdminAction(adminId, "AUDIT_LOGS_EXPORT_ATTEMPTED", 
            "Audit logs export attempted in format: " + format, 
            getClientIpAddress(request));
        
        // For now, redirect back with a message that export is not yet implemented
        model.addAttribute("message", "Export functionality will be implemented in a future version.");
        return "redirect:/admin/audit/logs";
    }
    
    /**
     * Extract client IP address from request, handling proxy headers.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * Convert AuditFilter to Map for service call.
     */
    private Map<String, Object> convertFilterToMap(AuditFilter filter) {
        Map<String, Object> filterMap = new HashMap<>();
        
        if (filter.getUserId() != null) {
            filterMap.put("userId", filter.getUserId());
        }
        if (filter.getEventType() != null) {
            filterMap.put("eventType", filter.getEventType());
        }
        if (filter.getStartDate() != null) {
            filterMap.put("startDate", filter.getStartDate());
        }
        if (filter.getEndDate() != null) {
            filterMap.put("endDate", filter.getEndDate());
        }
        if (filter.getAction() != null) {
            filterMap.put("action", filter.getAction());
        }
        if (filter.getIpAddress() != null) {
            filterMap.put("ipAddress", filter.getIpAddress());
        }
        filterMap.put("limit", filter.getLimit());
        filterMap.put("offset", filter.getOffset());
        
        return filterMap;
    }
}