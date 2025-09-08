package com.securevoting.exception;

/**
 * Exception thrown when cryptographic operations fail.
 * This includes encryption, decryption, key generation, and hash operations.
 */
public class CryptographyException extends RuntimeException {
    
    public CryptographyException(String message) {
        super(message);
    }
    
    public CryptographyException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public CryptographyException(Throwable cause) {
        super(cause);
    }
}