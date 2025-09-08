package com.securevoting.exception;

/**
 * Exception thrown when QR code generation or processing fails.
 */
public class QRCodeGenerationException extends RuntimeException {
    
    public QRCodeGenerationException(String message) {
        super(message);
    }
    
    public QRCodeGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}