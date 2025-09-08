package com.securevoting.repository;

import com.securevoting.model.Election;
import com.securevoting.model.ElectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for Election entity operations.
 */
@Repository
public interface ElectionRepository extends JpaRepository<Election, String> {
    
    /**
     * Find elections by status.
     */
    List<Election> findByStatus(ElectionStatus status);
    
    /**
     * Find elections by status ordered by start time.
     */
    List<Election> findByStatusOrderByStartTimeAsc(ElectionStatus status);
    
    /**
     * Find active elections (status = ACTIVE and current time is between start and end time).
     */
    @Query("SELECT e FROM Election e WHERE e.status = 'ACTIVE' AND :now BETWEEN e.startTime AND e.endTime")
    List<Election> findActiveElections(@Param("now") LocalDateTime now);
    
    /**
     * Find upcoming elections (status = ACTIVE and start time is in the future).
     */
    @Query("SELECT e FROM Election e WHERE e.status = 'ACTIVE' AND e.startTime > :now ORDER BY e.startTime ASC")
    List<Election> findUpcomingElections(@Param("now") LocalDateTime now);
    
    /**
     * Find completed elections (status = COMPLETED or end time has passed).
     */
    @Query("SELECT e FROM Election e WHERE e.status = 'COMPLETED' OR (e.status = 'ACTIVE' AND e.endTime < :now) ORDER BY e.endTime DESC")
    List<Election> findCompletedElections(@Param("now") LocalDateTime now);
    
    /**
     * Find elections created by a specific user.
     */
    List<Election> findByCreatedByOrderByCreatedAtDesc(String createdBy);
    
    /**
     * Find elections within a date range.
     */
    @Query("SELECT e FROM Election e WHERE e.startTime >= :startDate AND e.endTime <= :endDate ORDER BY e.startTime ASC")
    List<Election> findElectionsInDateRange(@Param("startDate") LocalDateTime startDate, 
                                          @Param("endDate") LocalDateTime endDate);
    
    /**
     * Count elections by status.
     */
    long countByStatus(ElectionStatus status);
    
    /**
     * Find elections that need status update (active elections that have ended).
     */
    @Query("SELECT e FROM Election e WHERE e.status = 'ACTIVE' AND e.endTime < :now")
    List<Election> findElectionsNeedingStatusUpdate(@Param("now") LocalDateTime now);
    
    /**
     * Search elections by title containing keyword.
     */
    @Query("SELECT e FROM Election e WHERE LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY e.createdAt DESC")
    List<Election> searchByTitle(@Param("keyword") String keyword);
}