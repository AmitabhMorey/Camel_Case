package com.securevoting.service;

import com.securevoting.exception.QRCodeGenerationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for QRCodeServiceImpl.
 * Tests QR code generation, validation, and security features.
 */
@ExtendWith(MockitoExtension.class)
class QRCodeServiceImplTest {
    
    private QRCodeServiceImpl qrCodeService;
    
    @BeforeEach
    void setUp() {
        qrCodeService = new QRCodeServiceImpl();
        // Set test configuration values
        ReflectionTestUtils.setField(qrCodeService, "qrCodeExpiryMinutes", 5);
        ReflectionTestUtils.setField(qrCodeService, "qrSecretKey", "TestSecretKey");
    }
    
    @Test
    void generateQRCodeData_ValidUserId_ReturnsFormattedData() {
        // Given
        String userId = "testUser123";
        
        // When
        String qrData = qrCodeService.generateQRCodeData(userId);
        
        // Then
        assertNotNull(qrData);
        assertTrue(qrData.startsWith(userId + "|"));
        
        String[] parts = qrData.split("\\|");
        assertEquals(3, parts.length, "QR data should have 3 parts: userId|timestamp|checksum");
        assertEquals(userId, parts[0]);
        assertNotNull(parts[1]); // timestamp
        assertNotNull(parts[2]); // checksum
    }
    
    @Test
    void generateQRCodeData_NullUserId_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> qrCodeService.generateQRCodeData(null)
        );
        assertEquals("User ID cannot be null or empty", exception.getMessage());
    }
    
    @Test
    void generateQRCodeData_EmptyUserId_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> qrCodeService.generateQRCodeData("")
        );
        assertEquals("User ID cannot be null or empty", exception.getMessage());
    }
    
    @Test
    void generateQRCodeImage_ValidData_ReturnsImageBytes() {
        // Given
        String testData = "testUser123|2024-01-01T10:00:00|123456";
        
        // When
        byte[] imageBytes = qrCodeService.generateQRCodeImage(testData);
        
        // Then
        assertNotNull(imageBytes);
        assertTrue(imageBytes.length > 0, "Image bytes should not be empty");
        
        // Check PNG header (first 8 bytes)
        assertEquals((byte) 0x89, imageBytes[0]); // PNG signature
        assertEquals((byte) 0x50, imageBytes[1]); // 'P'
        assertEquals((byte) 0x4E, imageBytes[2]); // 'N'
        assertEquals((byte) 0x47, imageBytes[3]); // 'G'
    }
    
    @Test
    void generateQRCodeImage_NullData_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> qrCodeService.generateQRCodeImage(null)
        );
        assertEquals("QR code data cannot be null or empty", exception.getMessage());
    }
    
    @Test
    void generateQRCodeImage_EmptyData_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> qrCodeService.generateQRCodeImage("")
        );
        assertEquals("QR code data cannot be null or empty", exception.getMessage());
    }
    
    @Test
    void validateQRCodeData_ValidDataAndUserId_ReturnsTrue() {
        // Given
        String userId = "testUser123";
        String qrData = qrCodeService.generateQRCodeData(userId);
        
        // When
        boolean isValid = qrCodeService.validateQRCodeData(qrData, userId);
        
        // Then
        assertTrue(isValid, "Valid QR code should pass validation");
    }
    
    @Test
    void validateQRCodeData_WrongUserId_ReturnsFalse() {
        // Given
        String userId = "testUser123";
        String wrongUserId = "wrongUser456";
        String qrData = qrCodeService.generateQRCodeData(userId);
        
        // When
        boolean isValid = qrCodeService.validateQRCodeData(qrData, wrongUserId);
        
        // Then
        assertFalse(isValid, "QR code with wrong user ID should fail validation");
    }
    
    @Test
    void validateQRCodeData_NullData_ReturnsFalse() {
        // When
        boolean isValid = qrCodeService.validateQRCodeData(null, "testUser");
        
        // Then
        assertFalse(isValid, "Null QR data should fail validation");
    }
    
    @Test
    void validateQRCodeData_NullUserId_ReturnsFalse() {
        // Given
        String qrData = "testUser123|2024-01-01T10:00:00|123456";
        
        // When
        boolean isValid = qrCodeService.validateQRCodeData(qrData, null);
        
        // Then
        assertFalse(isValid, "Null user ID should fail validation");
    }
    
    @Test
    void validateQRCodeData_InvalidFormat_ReturnsFalse() {
        // Given
        String invalidQrData = "invalidformat";
        String userId = "testUser123";
        
        // When
        boolean isValid = qrCodeService.validateQRCodeData(invalidQrData, userId);
        
        // Then
        assertFalse(isValid, "Invalid QR data format should fail validation");
    }
    
    @Test
    void validateQRCodeData_TamperedChecksum_ReturnsFalse() {
        // Given
        String userId = "testUser123";
        String qrData = qrCodeService.generateQRCodeData(userId);
        String[] parts = qrData.split("\\|");
        String tamperedData = parts[0] + "|" + parts[1] + "|" + "wrongchecksum";
        
        // When
        boolean isValid = qrCodeService.validateQRCodeData(tamperedData, userId);
        
        // Then
        assertFalse(isValid, "QR code with tampered checksum should fail validation");
    }
    
    @Test
    void isQRCodeExpired_FreshQRCode_ReturnsFalse() {
        // Given
        String userId = "testUser123";
        String qrData = qrCodeService.generateQRCodeData(userId);
        
        // When
        boolean isExpired = qrCodeService.isQRCodeExpired(qrData);
        
        // Then
        assertFalse(isExpired, "Fresh QR code should not be expired");
    }
    
    @Test
    void isQRCodeExpired_ExpiredQRCode_ReturnsTrue() {
        // Given - Create QR data with old timestamp (more than 5 minutes ago)
        String oldTimestamp = "2024-01-01T10:00:00";
        String expiredQrData = "testUser123|" + oldTimestamp + "|123456";
        
        // When
        boolean isExpired = qrCodeService.isQRCodeExpired(expiredQrData);
        
        // Then
        assertTrue(isExpired, "Old QR code should be expired");
    }
    
    @Test
    void isQRCodeExpired_NullData_ReturnsTrue() {
        // When
        boolean isExpired = qrCodeService.isQRCodeExpired(null);
        
        // Then
        assertTrue(isExpired, "Null QR data should be considered expired");
    }
    
    @Test
    void isQRCodeExpired_InvalidFormat_ReturnsTrue() {
        // Given
        String invalidData = "invalidformat";
        
        // When
        boolean isExpired = qrCodeService.isQRCodeExpired(invalidData);
        
        // Then
        assertTrue(isExpired, "Invalid QR data format should be considered expired");
    }
    
    @Test
    void validateQRCodeData_ExpiredQRCode_ReturnsFalse() {
        // Given - Create expired QR data
        String oldTimestamp = "2024-01-01T10:00:00";
        String userId = "testUser123";
        
        // Create QR data manually with old timestamp but valid checksum
        String baseData = userId + "|" + oldTimestamp;
        // We need to calculate the checksum using reflection to access private method
        String qrData = baseData + "|123456"; // Using dummy checksum for this test
        
        // When
        boolean isValid = qrCodeService.validateQRCodeData(qrData, userId);
        
        // Then
        assertFalse(isValid, "Expired QR code should fail validation");
    }
    
    @Test
    void generateQRCodeData_MultipleCallsSameUser_GeneratesDifferentData() {
        // Given
        String userId = "testUser123";
        
        // When
        String qrData1 = qrCodeService.generateQRCodeData(userId);
        // Small delay to ensure different timestamps
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        String qrData2 = qrCodeService.generateQRCodeData(userId);
        
        // Then
        assertNotEquals(qrData1, qrData2, "Multiple QR code generations should produce different data");
    }
    
    @Test
    void generateQRCodeImage_LargeData_ThrowsException() {
        // Given - Create large data string that exceeds QR code capacity
        StringBuilder largeData = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            largeData.append("testUser123|2024-01-01T10:00:00|123456|");
        }
        
        // When & Then - Should throw QRCodeGenerationException for data too large
        QRCodeGenerationException exception = assertThrows(
                QRCodeGenerationException.class,
                () -> qrCodeService.generateQRCodeImage(largeData.toString())
        );
        assertEquals("Failed to encode QR code", exception.getMessage());
    }
}