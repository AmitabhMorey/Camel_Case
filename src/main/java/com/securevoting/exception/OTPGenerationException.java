package com.securevoting.exception;

/**
 * Exception thrown when OTP generation fails or rate limits are exceeded.
 */
public class OTPGenerationException extends RuntimeException {
    
    public OTPGenerationException(String message) {
        super(message);
    }
    
    public OTPGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}