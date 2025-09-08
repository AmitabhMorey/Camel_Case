package com.securevoting.model;

import java.util.Objects;

/**
 * Data model representing an encrypted vote with associated cryptographic metadata.
 * Contains the encrypted vote data, initialization vector, hash for integrity verification,
 * and algorithm information.
 */
public class EncryptedVote {
    
    private String encryptedData;
    private String initializationVector;
    private String hash;
    private String algorithm;
    
    public EncryptedVote() {
    }
    
    public EncryptedVote(String encryptedData, String initializationVector, String hash, String algorithm) {
        this.encryptedData = encryptedData;
        this.initializationVector = initializationVector;
        this.hash = hash;
        this.algorithm = algorithm;
    }
    
    public String getEncryptedData() {
        return encryptedData;
    }
    
    public void setEncryptedData(String encryptedData) {
        this.encryptedData = encryptedData;
    }
    
    public String getInitializationVector() {
        return initializationVector;
    }
    
    public void setInitializationVector(String initializationVector) {
        this.initializationVector = initializationVector;
    }
    
    public String getHash() {
        return hash;
    }
    
    public void setHash(String hash) {
        this.hash = hash;
    }
    
    public String getAlgorithm() {
        return algorithm;
    }
    
    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EncryptedVote that = (EncryptedVote) o;
        return Objects.equals(encryptedData, that.encryptedData) &&
               Objects.equals(initializationVector, that.initializationVector) &&
               Objects.equals(hash, that.hash) &&
               Objects.equals(algorithm, that.algorithm);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(encryptedData, initializationVector, hash, algorithm);
    }
    
    @Override
    public String toString() {
        return "EncryptedVote{" +
               "algorithm='" + algorithm + '\'' +
               ", hasEncryptedData=" + (encryptedData != null && !encryptedData.isEmpty()) +
               ", hasIV=" + (initializationVector != null && !initializationVector.isEmpty()) +
               ", hasHash=" + (hash != null && !hash.isEmpty()) +
               '}';
    }
}