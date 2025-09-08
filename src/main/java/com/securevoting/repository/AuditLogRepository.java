package com.securevoting.repository;

import com.securevoting.model.AuditEventType;
import com.securevoting.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for AuditLog entity operations.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {
    
    /**
     * Find audit logs by user ID ordered by timestamp descending.
     */
    List<AuditLog> findByUserIdOrderByTimestampDesc(String userId);
    
    /**
     * Find audit logs by user ID with pagination.
     */
    Page<AuditLog> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable);
    
    /**
     * Find audit logs by event type ordered by timestamp descending.
     */
    List<AuditLog> findByEventTypeOrderByTimestampDesc(AuditEventType eventType);
    
    /**
     * Find audit logs by event type with pagination.
     */
    Page<AuditLog> findByEventTypeOrderByTimestampDesc(AuditEventType eventType, Pageable pageable);
    
    /**
     * Find audit logs within a time range.
     */
    @Query("SELECT a FROM AuditLog a WHERE a.timestamp BETWEEN :startTime AND :endTime ORDER BY a.timestamp DESC")
    List<AuditLog> findLogsInTimeRange(@Param("startTime") LocalDateTime startTime, 
                                      @Param("endTime") LocalDateTime endTime);
    
    /**
     * Find audit logs within a time range with pagination.
     */
    @Query("SELECT a FROM AuditLog a WHERE a.timestamp BETWEEN :startTime AND :endTime ORDER BY a.timestamp DESC")
    Page<AuditLog> findLogsInTimeRange(@Param("startTime") LocalDateTime startTime, 
                                      @Param("endTime") LocalDateTime endTime, 
                                      Pageable pageable);
    
    /**
     * Find audit logs by IP address.
     */
    List<AuditLog> findByIpAddressOrderByTimestampDesc(String ipAddress);
    
    /**
     * Find audit logs by session ID.
     */
    List<AuditLog> findBySessionIdOrderByTimestampDesc(String sessionId);
    
    /**
     * Find audit logs by action containing keyword.
     */
    @Query("SELECT a FROM AuditLog a WHERE LOWER(a.action) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY a.timestamp DESC")
    List<AuditLog> searchByAction(@Param("keyword") String keyword);
    
    /**
     * Find audit logs by resource ID and type.
     */
    List<AuditLog> findByResourceIdAndResourceTypeOrderByTimestampDesc(String resourceId, String resourceType);
    
    /**
     * Count logs by event type.
     */
    long countByEventType(AuditEventType eventType);
    
    /**
     * Count logs by user ID.
     */
    long countByUserId(String userId);
    
    /**
     * Count logs in time range.
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.timestamp BETWEEN :startTime AND :endTime")
    long countLogsInTimeRange(@Param("startTime") LocalDateTime startTime, 
                             @Param("endTime") LocalDateTime endTime);
    
    /**
     * Find recent security events.
     */
    @Query("SELECT a FROM AuditLog a WHERE a.eventType IN ('SECURITY_EVENT', 'LOGIN_FAILED', 'OTP_FAILED') AND a.timestamp >= :since ORDER BY a.timestamp DESC")
    List<AuditLog> findRecentSecurityEvents(@Param("since") LocalDateTime since);
    
    /**
     * Find failed login attempts by IP address.
     */
    @Query("SELECT a FROM AuditLog a WHERE a.eventType = 'LOGIN_FAILED' AND a.ipAddress = :ipAddress AND a.timestamp >= :since ORDER BY a.timestamp DESC")
    List<AuditLog> findFailedLoginsByIpAddress(@Param("ipAddress") String ipAddress, 
                                              @Param("since") LocalDateTime since);
    
    /**
     * Get audit statistics for a time period.
     */
    @Query("SELECT a.eventType, COUNT(a) FROM AuditLog a WHERE a.timestamp BETWEEN :startTime AND :endTime GROUP BY a.eventType")
    List<Object[]> getAuditStatistics(@Param("startTime") LocalDateTime startTime, 
                                     @Param("endTime") LocalDateTime endTime);
    
    /**
     * Find all audit logs with pagination (for admin view).
     */
    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);
    
    /**
     * Find audit logs by user ID.
     */
    List<AuditLog> findByUserId(String userId);
    
    /**
     * Find audit logs by user ID and timestamp range.
     */
    List<AuditLog> findByUserIdAndTimestampBetween(String userId, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find audit logs by event type.
     */
    List<AuditLog> findByEventType(AuditEventType eventType);
    
    /**
     * Find audit logs by event type and timestamp range.
     */
    List<AuditLog> findByEventTypeAndTimestampBetween(AuditEventType eventType, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find audit logs with complex filtering.
     */
    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:userId IS NULL OR a.userId = :userId) AND " +
           "(:eventType IS NULL OR a.eventType = :eventType) AND " +
           "(:startDate IS NULL OR a.timestamp >= :startDate) AND " +
           "(:endDate IS NULL OR a.timestamp <= :endDate) AND " +
           "(:ipAddress IS NULL OR a.ipAddress = :ipAddress) " +
           "ORDER BY a.timestamp DESC")
    List<AuditLog> findByFilters(@Param("userId") String userId,
                                @Param("eventType") AuditEventType eventType,
                                @Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate,
                                @Param("ipAddress") String ipAddress);
}