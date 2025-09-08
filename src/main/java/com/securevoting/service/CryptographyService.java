package com.securevoting.service;

import com.securevoting.model.EncryptedVote;

/**
 * Service interface for cryptographic operations including vote encryption and hash generation.
 * Provides secure encryption using AES-256-GCM and hash verification using SHA-256.
 */
public interface CryptographyService {
    
    /**
     * Encrypts vote data using AES-256-GCM encryption with a unique initialization vector.
     * 
     * @param voteData the plain text vote data to encrypt
     * @param electionId the election identifier used for key derivation
     * @return EncryptedVote containing encrypted data, IV, and hash
     * @throws CryptographyException if encryption fails
     */
    EncryptedVote encryptVote(String voteData, String electionId);
    
    /**
     * Decrypts an encrypted vote back to plain text.
     * 
     * @param encryptedVote the encrypted vote data to decrypt
     * @return the decrypted vote data as plain text
     * @throws CryptographyException if decryption fails or data is tampered
     */
    String decryptVote(EncryptedVote encryptedVote);
    
    /**
     * Generates a SHA-256 hash of the provided data.
     * 
     * @param data the data to hash
     * @return the hexadecimal representation of the hash
     * @throws CryptographyException if hash generation fails
     */
    String generateHash(String data);
    
    /**
     * Verifies that the provided data matches the given hash.
     * 
     * @param data the original data to verify
     * @param hash the expected hash value
     * @return true if the data matches the hash, false otherwise
     */
    boolean verifyHash(String data, String hash);
    
    /**
     * Generates a secure encryption key for the given election.
     * 
     * @param electionId the election identifier
     * @return the generated encryption key as byte array
     * @throws CryptographyException if key generation fails
     */
    byte[] generateElectionKey(String electionId);
}