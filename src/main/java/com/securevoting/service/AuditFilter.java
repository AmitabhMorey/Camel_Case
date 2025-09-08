package com.securevoting.service;

import com.securevoting.model.AuditEventType;
import java.time.LocalDateTime;

/**
 * Filter criteria for audit log queries.
 * Allows filtering by various audit log attributes.
 */
public class AuditFilter {
    
    private String userId;
    private AuditEventType eventType;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String action;
    private String ipAddress;
    private int limit = 100; // Default limit
    private int offset = 0;  // Default offset for pagination
    
    // Constructors
    public AuditFilter() {}
    
    public AuditFilter(String userId, AuditEventType eventType, LocalDateTime startDate, LocalDateTime endDate) {
        this.userId = userId;
        this.eventType = eventType;
        this.startDate = startDate;
        this.endDate = endDate;
    }
    
    // Getters and Setters
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public AuditEventType getEventType() {
        return eventType;
    }
    
    public void setEventType(AuditEventType eventType) {
        this.eventType = eventType;
    }
    
    public LocalDateTime getStartDate() {
        return startDate;
    }
    
    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }
    
    public LocalDateTime getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public int getLimit() {
        return limit;
    }
    
    public void setLimit(int limit) {
        this.limit = Math.max(1, Math.min(limit, 1000)); // Ensure reasonable limits
    }
    
    public int getOffset() {
        return offset;
    }
    
    public void setOffset(int offset) {
        this.offset = Math.max(0, offset);
    }
    
    /**
     * Checks if any filter criteria are set.
     * 
     * @return true if any filter is applied, false if this is an empty filter
     */
    public boolean hasFilters() {
        return userId != null || eventType != null || startDate != null || 
               endDate != null || action != null || ipAddress != null;
    }
    
    /**
     * Creates a filter for recent audit logs (last 24 hours).
     * 
     * @return AuditFilter configured for recent logs
     */
    public static AuditFilter recentLogs() {
        AuditFilter filter = new AuditFilter();
        filter.setStartDate(LocalDateTime.now().minusDays(1));
        filter.setLimit(50);
        return filter;
    }
    
    /**
     * Creates a filter for a specific user's audit logs.
     * 
     * @param userId the user ID to filter by
     * @return AuditFilter configured for the user
     */
    public static AuditFilter forUser(String userId) {
        AuditFilter filter = new AuditFilter();
        filter.setUserId(userId);
        return filter;
    }
    
    /**
     * Creates a filter for a specific event type.
     * 
     * @param eventType the event type to filter by
     * @return AuditFilter configured for the event type
     */
    public static AuditFilter forEventType(AuditEventType eventType) {
        AuditFilter filter = new AuditFilter();
        filter.setEventType(eventType);
        return filter;
    }
    
    @Override
    public String toString() {
        return "AuditFilter{" +
                "userId='" + userId + '\'' +
                ", eventType=" + eventType +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", action='" + action + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", limit=" + limit +
                ", offset=" + offset +
                '}';
    }
}