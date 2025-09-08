package com.securevoting.service;

/**
 * Service interface for QR code generation and validation operations.
 * Provides methods to generate QR codes for user authentication and validate them.
 */
public interface QRCodeService {
    
    /**
     * Generates QR code data string containing user identification and timestamp.
     * 
     * @param userId the unique identifier of the user
     * @return formatted QR code data string
     */
    String generateQRCodeData(String userId);
    
    /**
     * Generates QR code image as byte array from the provided data.
     * 
     * @param data the data to encode in the QR code
     * @return byte array representing the QR code image
     * @throws QRCodeGenerationException if QR code generation fails
     */
    byte[] generateQRCodeImage(String data);
    
    /**
     * Validates QR code data against user ID with expiry and security checks.
     * 
     * @param data the QR code data to validate
     * @param userId the user ID to validate against
     * @return true if QR code is valid and not expired, false otherwise
     */
    boolean validateQRCodeData(String data, String userId);
    
    /**
     * Checks if QR code data has expired based on timestamp.
     * 
     * @param data the QR code data containing timestamp
     * @return true if expired, false otherwise
     */
    boolean isQRCodeExpired(String data);
}