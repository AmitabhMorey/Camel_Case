package com.securevoting.service;

import com.securevoting.exception.CryptographyException;
import com.securevoting.model.EncryptedVote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of CryptographyService providing AES-256-GCM encryption and SHA-256 hashing.
 * Manages encryption keys per election and ensures vote integrity through cryptographic hashing.
 */
@Service
public class CryptographyServiceImpl implements CryptographyService {
    
    private static final Logger logger = LoggerFactory.getLogger(CryptographyServiceImpl.class);
    
    private static final String AES_ALGORITHM = "AES";
    private static final String AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 16; // 128 bits
    private static final int AES_KEY_LENGTH = 256; // 256 bits
    
    private final SecureRandom secureRandom;
    private final ConcurrentHashMap<String, SecretKey> electionKeys;
    
    public CryptographyServiceImpl() {
        this.secureRandom = new SecureRandom();
        this.electionKeys = new ConcurrentHashMap<>();
    }
    
    @Override
    public EncryptedVote encryptVote(String voteData, String electionId) {
        if (voteData == null || voteData.trim().isEmpty()) {
            throw new CryptographyException("Vote data cannot be null or empty");
        }
        if (electionId == null || electionId.trim().isEmpty()) {
            throw new CryptographyException("Election ID cannot be null or empty");
        }
        
        try {
            logger.debug("Encrypting vote for election: {}", electionId);
            
            // Get or generate election key
            SecretKey secretKey = getOrCreateElectionKey(electionId);
            
            // Generate unique IV for this encryption
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            
            // Initialize cipher for encryption
            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);
            
            // Encrypt the vote data
            byte[] encryptedBytes = cipher.doFinal(voteData.getBytes(StandardCharsets.UTF_8));
            
            // Encode encrypted data and IV to Base64
            String encryptedData = Base64.getEncoder().encodeToString(encryptedBytes);
            String ivString = Base64.getEncoder().encodeToString(iv);
            
            // Generate hash of original vote data for integrity verification
            String hash = generateHash(voteData);
            
            logger.debug("Vote encryption completed successfully for election: {}", electionId);
            
            return new EncryptedVote(encryptedData, ivString, hash, AES_GCM_TRANSFORMATION);
            
        } catch (Exception e) {
            logger.error("Failed to encrypt vote for election: {}", electionId, e);
            throw new CryptographyException("Vote encryption failed", e);
        }
    }
    
    @Override
    public String decryptVote(EncryptedVote encryptedVote) {
        if (encryptedVote == null) {
            throw new CryptographyException("Encrypted vote cannot be null");
        }
        if (encryptedVote.getEncryptedData() == null || encryptedVote.getInitializationVector() == null) {
            throw new CryptographyException("Encrypted data and IV cannot be null");
        }
        
        try {
            logger.debug("Decrypting vote with algorithm: {}", encryptedVote.getAlgorithm());
            
            // Decode Base64 encoded data
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedVote.getEncryptedData());
            byte[] iv = Base64.getDecoder().decode(encryptedVote.getInitializationVector());
            
            // For this implementation, we'll use a default key since we don't have election context
            // In a real implementation, this would require the election ID to retrieve the correct key
            SecretKey secretKey = generateDefaultKey();
            
            // Initialize cipher for decryption
            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);
            
            // Decrypt the data
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            String decryptedData = new String(decryptedBytes, StandardCharsets.UTF_8);
            
            // Verify hash integrity if hash is provided
            if (encryptedVote.getHash() != null) {
                if (!verifyHash(decryptedData, encryptedVote.getHash())) {
                    throw new CryptographyException("Vote integrity verification failed - hash mismatch");
                }
            }
            
            logger.debug("Vote decryption completed successfully");
            return decryptedData;
            
        } catch (CryptographyException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to decrypt vote", e);
            throw new CryptographyException("Vote decryption failed", e);
        }
    }
    
    /**
     * Decrypts an encrypted vote using the specified election key.
     * 
     * @param encryptedVote the encrypted vote data to decrypt
     * @param electionId the election identifier to retrieve the correct key
     * @return the decrypted vote data as plain text
     * @throws CryptographyException if decryption fails or data is tampered
     */
    public String decryptVote(EncryptedVote encryptedVote, String electionId) {
        if (encryptedVote == null) {
            throw new CryptographyException("Encrypted vote cannot be null");
        }
        if (encryptedVote.getEncryptedData() == null || encryptedVote.getInitializationVector() == null) {
            throw new CryptographyException("Encrypted data and IV cannot be null");
        }
        if (electionId == null || electionId.trim().isEmpty()) {
            throw new CryptographyException("Election ID cannot be null or empty");
        }
        
        try {
            logger.debug("Decrypting vote for election: {} with algorithm: {}", electionId, encryptedVote.getAlgorithm());
            
            // Decode Base64 encoded data
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedVote.getEncryptedData());
            byte[] iv = Base64.getDecoder().decode(encryptedVote.getInitializationVector());
            
            // Get the election-specific key
            SecretKey secretKey = getOrCreateElectionKey(electionId);
            
            // Initialize cipher for decryption
            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);
            
            // Decrypt the data
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            String decryptedData = new String(decryptedBytes, StandardCharsets.UTF_8);
            
            // Verify hash integrity if hash is provided
            if (encryptedVote.getHash() != null) {
                if (!verifyHash(decryptedData, encryptedVote.getHash())) {
                    throw new CryptographyException("Vote integrity verification failed - hash mismatch");
                }
            }
            
            logger.debug("Vote decryption completed successfully for election: {}", electionId);
            return decryptedData;
            
        } catch (CryptographyException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to decrypt vote for election: {}", electionId, e);
            throw new CryptographyException("Vote decryption failed", e);
        }
    }
    
    @Override
    public String generateHash(String data) {
        if (data == null) {
            throw new CryptographyException("Data to hash cannot be null");
        }
        
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Hash algorithm not available: {}", HASH_ALGORITHM, e);
            throw new CryptographyException("Hash generation failed", e);
        }
    }
    
    @Override
    public boolean verifyHash(String data, String hash) {
        if (data == null || hash == null) {
            return false;
        }
        
        try {
            String computedHash = generateHash(data);
            return computedHash.equals(hash);
        } catch (Exception e) {
            logger.warn("Hash verification failed", e);
            return false;
        }
    }
    
    @Override
    public byte[] generateElectionKey(String electionId) {
        if (electionId == null || electionId.trim().isEmpty()) {
            throw new CryptographyException("Election ID cannot be null or empty");
        }
        
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(AES_ALGORITHM);
            keyGenerator.init(AES_KEY_LENGTH);
            SecretKey secretKey = keyGenerator.generateKey();
            
            // Store the key for this election
            electionKeys.put(electionId, secretKey);
            
            logger.debug("Generated new encryption key for election: {}", electionId);
            return secretKey.getEncoded();
            
        } catch (NoSuchAlgorithmException e) {
            logger.error("AES algorithm not available", e);
            throw new CryptographyException("Key generation failed", e);
        }
    }
    
    /**
     * Gets existing election key or creates a new one if it doesn't exist.
     */
    private SecretKey getOrCreateElectionKey(String electionId) {
        return electionKeys.computeIfAbsent(electionId, id -> {
            try {
                KeyGenerator keyGenerator = KeyGenerator.getInstance(AES_ALGORITHM);
                keyGenerator.init(AES_KEY_LENGTH);
                return keyGenerator.generateKey();
            } catch (NoSuchAlgorithmException e) {
                throw new CryptographyException("Failed to generate key for election: " + id, e);
            }
        });
    }
    
    /**
     * Generates a default key for testing purposes.
     * In production, this should be replaced with proper key management.
     */
    private SecretKey generateDefaultKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(AES_ALGORITHM);
            keyGenerator.init(AES_KEY_LENGTH);
            return keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new CryptographyException("Failed to generate default key", e);
        }
    }
    
    /**
     * Converts byte array to hexadecimal string representation.
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}