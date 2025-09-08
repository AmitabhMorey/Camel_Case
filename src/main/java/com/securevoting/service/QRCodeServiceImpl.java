package com.securevoting.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.securevoting.exception.QRCodeGenerationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of QRCodeService for generating and validating QR codes.
 * Uses ZXing library for QR code operations with security and expiry features.
 */
@Service
public class QRCodeServiceImpl implements QRCodeService {
    
    private static final Logger logger = LoggerFactory.getLogger(QRCodeServiceImpl.class);
    
    private static final String QR_DATA_SEPARATOR = "|";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final int QR_CODE_WIDTH = 300;
    private static final int QR_CODE_HEIGHT = 300;
    
    @Value("${app.qr.expiry.minutes:5}")
    private int qrCodeExpiryMinutes;
    
    @Value("${app.qr.secret.key:SecureVotingQRSecret2024}")
    private String qrSecretKey;
    
    @Override
    public String generateQRCodeData(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        
        LocalDateTime timestamp = LocalDateTime.now();
        String timestampStr = timestamp.format(TIMESTAMP_FORMATTER);
        
        // Format: userId|timestamp|checksum
        String baseData = userId + QR_DATA_SEPARATOR + timestampStr;
        String checksum = generateChecksum(baseData);
        
        String qrData = baseData + QR_DATA_SEPARATOR + checksum;
        
        logger.debug("Generated QR code data for user: {}", userId);
        return qrData;
    }
    
    @Override
    public byte[] generateQRCodeImage(String data) {
        if (data == null || data.trim().isEmpty()) {
            throw new IllegalArgumentException("QR code data cannot be null or empty");
        }
        
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            
            BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, 
                    QR_CODE_WIDTH, QR_CODE_HEIGHT, hints);
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            
            byte[] qrCodeImage = outputStream.toByteArray();
            logger.debug("Generated QR code image with {} bytes", qrCodeImage.length);
            
            return qrCodeImage;
            
        } catch (WriterException e) {
            logger.error("Failed to encode QR code data: {}", data, e);
            throw new QRCodeGenerationException("Failed to encode QR code", e);
        } catch (IOException e) {
            logger.error("Failed to write QR code image to stream", e);
            throw new QRCodeGenerationException("Failed to generate QR code image", e);
        }
    }
    
    @Override
    public boolean validateQRCodeData(String data, String userId) {
        if (data == null || userId == null) {
            logger.warn("QR code validation failed: null data or userId");
            return false;
        }
        
        try {
            String[] parts = data.split("\\" + QR_DATA_SEPARATOR);
            if (parts.length != 3) {
                logger.warn("Invalid QR code format: expected 3 parts, got {}", parts.length);
                return false;
            }
            
            String qrUserId = parts[0];
            String timestampStr = parts[1];
            String providedChecksum = parts[2];
            
            // Validate user ID match
            if (!userId.equals(qrUserId)) {
                logger.warn("QR code user ID mismatch: expected {}, got {}", userId, qrUserId);
                return false;
            }
            
            // Validate checksum
            String baseData = qrUserId + QR_DATA_SEPARATOR + timestampStr;
            String expectedChecksum = generateChecksum(baseData);
            if (!expectedChecksum.equals(providedChecksum)) {
                logger.warn("QR code checksum validation failed for user: {}", userId);
                return false;
            }
            
            // Check expiry
            if (isQRCodeExpired(data)) {
                logger.warn("QR code expired for user: {}", userId);
                return false;
            }
            
            logger.debug("QR code validation successful for user: {}", userId);
            return true;
            
        } catch (Exception e) {
            logger.error("Error validating QR code for user: {}", userId, e);
            return false;
        }
    }
    
    @Override
    public boolean isQRCodeExpired(String data) {
        if (data == null) {
            return true;
        }
        
        try {
            String[] parts = data.split("\\" + QR_DATA_SEPARATOR);
            if (parts.length < 2) {
                return true;
            }
            
            String timestampStr = parts[1];
            LocalDateTime qrTimestamp = LocalDateTime.parse(timestampStr, TIMESTAMP_FORMATTER);
            LocalDateTime now = LocalDateTime.now();
            
            long minutesElapsed = ChronoUnit.MINUTES.between(qrTimestamp, now);
            boolean expired = minutesElapsed > qrCodeExpiryMinutes;
            
            if (expired) {
                logger.debug("QR code expired: {} minutes elapsed, limit is {}", 
                        minutesElapsed, qrCodeExpiryMinutes);
            }
            
            return expired;
            
        } catch (Exception e) {
            logger.error("Error checking QR code expiry", e);
            return true;
        }
    }
    
    /**
     * Generates a simple checksum for QR code data integrity.
     * Uses a combination of data hash and secret key for basic security.
     */
    private String generateChecksum(String data) {
        String combined = data + qrSecretKey;
        return String.valueOf(Math.abs(combined.hashCode()));
    }
}