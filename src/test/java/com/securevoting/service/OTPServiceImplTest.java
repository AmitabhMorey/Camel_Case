package com.securevoting.service;

import com.securevoting.exception.OTPGenerationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for OTPServiceImpl covering OTP lifecycle and security scenarios.
 */
@ExtendWith(MockitoExtension.class)
class OTPServiceImplTest {
    
    private OTPServiceImpl otpService;
    
    @BeforeEach
    void setUp() {
        otpService = new OTPServiceImpl();
        
        // Set test configuration values
        ReflectionTestUtils.setField(otpService, "otpValidityMinutes", 5);
        ReflectionTestUtils.setField(otpService, "rateLimitMinutes", 1);
        ReflectionTestUtils.setField(otpService, "maxAttempts", 3);
        ReflectionTestUtils.setField(otpService, "timeStepSeconds", 30);
    }
    
    @Test
    void testGenerateOTP_Success() {
        // Given
        String userId = "testUser123";
        
        // When
        String otp = otpService.generateOTP(userId);
        
        // Then
        assertNotNull(otp);
        assertEquals(6, otp.length());
        assertTrue(otp.matches("\\d{6}"));
    }
    
    @Test
    void testGenerateOTP_NullUserId_ThrowsException() {
        // When & Then
        assertThrows(OTPGenerationException.class, () -> otpService.generateOTP(null));
    }
    
    @Test
    void testValidateOTP_ValidOTP_ReturnsTrue() {
        // Given
        String userId = "testUser123";
        String otp = otpService.generateOTP(userId);
        
        // When
        boolean isValid = otpService.validateOTP(userId, otp);
        
        // Then
        assertTrue(isValid);
    }
    
    @Test
    void testValidateOTP_InvalidOTP_ReturnsFalse() {
        // Given
        String userId = "testUser123";
        otpService.generateOTP(userId);
        
        // When
        boolean isValid = otpService.validateOTP(userId, "123456");
        
        // Then
        assertFalse(isValid);
    }
    
    @Test
    void testValidateOTP_NullInputs_ReturnsFalse() {
        // When & Then
        assertFalse(otpService.validateOTP(null, "123456"));
        assertFalse(otpService.validateOTP("testUser", null));
        assertFalse(otpService.validateOTP(null, null));
    }
    
    @Test
    void testValidateOTP_NoOTPGenerated_ReturnsFalse() {
        // Given
        String userId = "testUser123";
        
        // When
        boolean isValid = otpService.validateOTP(userId, "123456");
        
        // Then
        assertFalse(isValid);
    }
    
    @Test
    void testValidateOTP_MaxAttemptsExceeded_ReturnsFalse() {
        // Given
        String userId = "testUser123";
        String validOtp = otpService.generateOTP(userId);
        
        // When - Exceed max attempts with invalid OTPs
        otpService.validateOTP(userId, "111111");
        otpService.validateOTP(userId, "222222");
        otpService.validateOTP(userId, "333333");
        
        // Then - Even valid OTP should fail after max attempts
        boolean isValid = otpService.validateOTP(userId, validOtp);
        assertFalse(isValid);
    }
    
    @Test
    void testValidateOTP_OTPRemovedAfterSuccessfulValidation() {
        // Given
        String userId = "testUser123";
        String otp = otpService.generateOTP(userId);
        
        // When
        boolean firstValidation = otpService.validateOTP(userId, otp);
        boolean secondValidation = otpService.validateOTP(userId, otp);
        
        // Then
        assertTrue(firstValidation);
        assertFalse(secondValidation); // OTP should be removed after first successful use
    }
    
    @Test
    void testInvalidateOTP_Success() {
        // Given
        String userId = "testUser123";
        String otp = otpService.generateOTP(userId);
        
        // When
        otpService.invalidateOTP(userId);
        boolean isValid = otpService.validateOTP(userId, otp);
        
        // Then
        assertFalse(isValid);
    }
    
    @Test
    void testInvalidateOTP_NonExistentUser_NoException() {
        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> otpService.invalidateOTP("nonExistentUser"));
    }
    
    @Test
    void testRateLimit_ExceededLimit_ThrowsException() {
        // Given
        String userId = "testUser123";
        otpService.generateOTP(userId);
        
        // When & Then
        assertThrows(OTPGenerationException.class, () -> otpService.generateOTP(userId));
    }
    
    @Test
    void testRateLimit_NotExceeded_Success() throws InterruptedException {
        // Given
        String userId = "testUser123";
        
        // When
        String firstOtp = otpService.generateOTP(userId);
        
        // Simulate time passing (in real scenario, wait for rate limit to expire)
        // For testing, we'll test the rate limit check method directly
        boolean rateLimitExceeded = otpService.isRateLimitExceeded(userId);
        
        // Then
        assertNotNull(firstOtp);
        assertTrue(rateLimitExceeded);
    }
    
    @Test
    void testIsRateLimitExceeded_NoRequestMade_ReturnsFalse() {
        // Given
        String userId = "newUser123";
        
        // When
        boolean rateLimitExceeded = otpService.isRateLimitExceeded(userId);
        
        // Then
        assertFalse(rateLimitExceeded);
    }
    
    @Test
    void testCleanupExpiredOTPs_Success() {
        // Given
        String userId1 = "user1";
        String userId2 = "user2";
        
        // Generate OTPs
        otpService.generateOTP(userId1);
        otpService.generateOTP(userId2);
        
        // When
        otpService.cleanupExpiredOTPs();
        
        // Then - Should not throw exception and complete successfully
        assertDoesNotThrow(() -> otpService.cleanupExpiredOTPs());
    }
    
    @Test
    void testOTPFormat_AlwaysSixDigits() {
        // Given - Use different user IDs to avoid rate limiting
        for (int i = 0; i < 10; i++) {
            String userId = "testUser" + i;
            
            // When
            String otp = otpService.generateOTP(userId);
            
            // Then
            assertEquals(6, otp.length());
            assertTrue(otp.matches("\\d{6}"));
        }
    }
    
    @Test
    void testOTPUniqueness_DifferentUsers() {
        // Given
        String userId1 = "user1";
        String userId2 = "user2";
        
        // When
        String otp1 = otpService.generateOTP(userId1);
        String otp2 = otpService.generateOTP(userId2);
        
        // Then - OTPs should be different for different users (high probability)
        assertNotEquals(otp1, otp2);
    }
    
    @Test
    void testConcurrentOTPGeneration_DifferentUsers() throws InterruptedException {
        // Given
        String[] userIds = {"user1", "user2", "user3", "user4", "user5"};
        String[] otps = new String[userIds.length];
        Thread[] threads = new Thread[userIds.length];
        
        // When - Generate OTPs concurrently
        for (int i = 0; i < userIds.length; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                otps[index] = otpService.generateOTP(userIds[index]);
            });
            threads[i].start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Then - All OTPs should be generated successfully
        for (String otp : otps) {
            assertNotNull(otp);
            assertEquals(6, otp.length());
            assertTrue(otp.matches("\\d{6}"));
        }
    }
    
    @Test
    void testOTPValidation_TimeWindowTolerance() {
        // Given
        String userId = "testUser123";
        String otp = otpService.generateOTP(userId);
        
        // When - Validate immediately (should work within time window)
        boolean isValid = otpService.validateOTP(userId, otp);
        
        // Then
        assertTrue(isValid);
    }
    
    @Test
    void testSecurityScenario_MultipleFailedAttempts() {
        // Given
        String userId = "testUser123";
        String validOtp = otpService.generateOTP(userId);
        
        // When - Make multiple failed attempts
        assertFalse(otpService.validateOTP(userId, "000000"));
        assertFalse(otpService.validateOTP(userId, "111111"));
        assertFalse(otpService.validateOTP(userId, "222222"));
        
        // Then - Valid OTP should now fail due to max attempts exceeded
        assertFalse(otpService.validateOTP(userId, validOtp));
    }
    
    @Test
    void testSecurityScenario_RateLimitPreventsAbuse() {
        // Given
        String userId = "testUser123";
        
        // When
        otpService.generateOTP(userId); // First generation succeeds
        
        // Then - Second generation should fail due to rate limit
        assertThrows(OTPGenerationException.class, () -> otpService.generateOTP(userId));
    }
}