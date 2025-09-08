package com.securevoting.repository;

import com.securevoting.model.User;
import com.securevoting.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User entity operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {
    
    /**
     * Find user by username.
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Find user by email.
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Find user by username or email.
     */
    @Query("SELECT u FROM User u WHERE u.username = :identifier OR u.email = :identifier")
    Optional<User> findByUsernameOrEmail(@Param("identifier") String identifier);
    
    /**
     * Check if username exists.
     */
    boolean existsByUsername(String username);
    
    /**
     * Check if email exists.
     */
    boolean existsByEmail(String email);
    
    /**
     * Find all users by role.
     */
    List<User> findByRole(UserRole role);
    
    /**
     * Find all enabled users.
     */
    List<User> findByEnabledTrue();
    
    /**
     * Find users created after a specific date.
     */
    List<User> findByCreatedAtAfter(LocalDateTime date);
    
    /**
     * Count users by role.
     */
    long countByRole(UserRole role);
    
    /**
     * Count enabled users.
     */
    long countByEnabledTrue();
    
    /**
     * Find users with QR code secret.
     */
    @Query("SELECT u FROM User u WHERE u.qrCodeSecret IS NOT NULL")
    List<User> findUsersWithQrCodeSecret();
}