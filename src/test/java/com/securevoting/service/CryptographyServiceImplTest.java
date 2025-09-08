package com.securevoting.service;

import com.securevoting.exception.CryptographyException;
import com.securevoting.model.EncryptedVote;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for CryptographyServiceImpl.
 * Tests encryption, decryption, hashing, and error handling scenarios.
 */
class CryptographyServiceImplTest {
    
    private CryptographyService cryptographyService;
    
    @BeforeEach
    void setUp() {
        cryptographyService = new CryptographyServiceImpl();
    }
    
    @Nested
    @DisplayName("Vote Encryption Tests")
    class VoteEncryptionTests {
        
        @Test
        @DisplayName("Should encrypt vote data successfully")
        void shouldEncryptVoteDataSuccessfully() {
            // Given
            String voteData = "candidate-123";
            String electionId = "election-456";
            
            // When
            EncryptedVote encryptedVote = cryptographyService.encryptVote(voteData, electionId);
            
            // Then
            assertNotNull(encryptedVote);
            assertNotNull(encryptedVote.getEncryptedData());
            assertNotNull(encryptedVote.getInitializationVector());
            assertNotNull(encryptedVote.getHash());
            assertEquals("AES/GCM/NoPadding", encryptedVote.getAlgorithm());
            
            // Encrypted data should be different from original
            assertNotEquals(voteData, encryptedVote.getEncryptedData());
        }
        
        @Test
        @DisplayName("Should generate unique IVs for each encryption")
        void shouldGenerateUniqueIVsForEachEncryption() {
            // Given
            String voteData = "candidate-123";
            String electionId = "election-456";
            
            // When
            EncryptedVote vote1 = cryptographyService.encryptVote(voteData, electionId);
            EncryptedVote vote2 = cryptographyService.encryptVote(voteData, electionId);
            
            // Then
            assertNotEquals(vote1.getInitializationVector(), vote2.getInitializationVector());
            assertNotEquals(vote1.getEncryptedData(), vote2.getEncryptedData());
        }
        
        @Test
        @DisplayName("Should throw exception for null vote data")
        void shouldThrowExceptionForNullVoteData() {
            // Given
            String electionId = "election-456";
            
            // When & Then
            CryptographyException exception = assertThrows(CryptographyException.class, 
                () -> cryptographyService.encryptVote(null, electionId));
            
            assertTrue(exception.getMessage().contains("Vote data cannot be null or empty"));
        }
        
        @Test
        @DisplayName("Should throw exception for empty vote data")
        void shouldThrowExceptionForEmptyVoteData() {
            // Given
            String electionId = "election-456";
            
            // When & Then
            CryptographyException exception = assertThrows(CryptographyException.class, 
                () -> cryptographyService.encryptVote("", electionId));
            
            assertTrue(exception.getMessage().contains("Vote data cannot be null or empty"));
        }
        
        @Test
        @DisplayName("Should throw exception for null election ID")
        void shouldThrowExceptionForNullElectionId() {
            // Given
            String voteData = "candidate-123";
            
            // When & Then
            CryptographyException exception = assertThrows(CryptographyException.class, 
                () -> cryptographyService.encryptVote(voteData, null));
            
            assertTrue(exception.getMessage().contains("Election ID cannot be null or empty"));
        }
    }
    
    @Nested
    @DisplayName("Vote Decryption Tests")
    class VoteDecryptionTests {
        
        @Test
        @DisplayName("Should decrypt vote data successfully")
        void shouldDecryptVoteDataSuccessfully() {
            // Given
            String originalVoteData = "candidate-789";
            String electionId = "election-123";
            
            // Encrypt first
            EncryptedVote encryptedVote = cryptographyService.encryptVote(originalVoteData, electionId);
            
            // When
            String decryptedData = ((CryptographyServiceImpl) cryptographyService).decryptVote(encryptedVote, electionId);
            
            // Then
            assertEquals(originalVoteData, decryptedData);
        }
        
        @Test
        @DisplayName("Should throw exception for null encrypted vote")
        void shouldThrowExceptionForNullEncryptedVote() {
            // When & Then
            CryptographyException exception = assertThrows(CryptographyException.class, 
                () -> cryptographyService.decryptVote(null));
            
            assertTrue(exception.getMessage().contains("Encrypted vote cannot be null"));
        }
        
        @Test
        @DisplayName("Should throw exception for encrypted vote with null data")
        void shouldThrowExceptionForEncryptedVoteWithNullData() {
            // Given
            EncryptedVote encryptedVote = new EncryptedVote();
            encryptedVote.setEncryptedData(null);
            encryptedVote.setInitializationVector("validIV");
            
            // When & Then
            CryptographyException exception = assertThrows(CryptographyException.class, 
                () -> cryptographyService.decryptVote(encryptedVote));
            
            assertTrue(exception.getMessage().contains("Encrypted data and IV cannot be null"));
        }
        
        @Test
        @DisplayName("Should throw exception when decrypting with wrong election ID")
        void shouldThrowExceptionWhenDecryptingWithWrongElectionId() {
            // Given
            String originalVoteData = "candidate-456";
            String correctElectionId = "election-correct";
            String wrongElectionId = "election-wrong";
            
            EncryptedVote encryptedVote = cryptographyService.encryptVote(originalVoteData, correctElectionId);
            
            // When & Then
            CryptographyException exception = assertThrows(CryptographyException.class, 
                () -> ((CryptographyServiceImpl) cryptographyService).decryptVote(encryptedVote, wrongElectionId));
            
            assertTrue(exception.getMessage().contains("Vote decryption failed"));
        }
        
        @Test
        @DisplayName("Should throw exception for null election ID in decryption")
        void shouldThrowExceptionForNullElectionIdInDecryption() {
            // Given
            EncryptedVote encryptedVote = new EncryptedVote();
            encryptedVote.setEncryptedData("validData");
            encryptedVote.setInitializationVector("validIV");
            
            // When & Then
            CryptographyException exception = assertThrows(CryptographyException.class, 
                () -> ((CryptographyServiceImpl) cryptographyService).decryptVote(encryptedVote, null));
            
            assertTrue(exception.getMessage().contains("Election ID cannot be null or empty"));
        }
        
        @Test
        @DisplayName("Should verify hash integrity during decryption")
        void shouldVerifyHashIntegrityDuringDecryption() {
            // Given
            String originalVoteData = "candidate-456";
            String electionId = "election-789";
            
            EncryptedVote encryptedVote = cryptographyService.encryptVote(originalVoteData, electionId);
            
            // Tamper with the hash
            encryptedVote.setHash("tampered-hash");
            
            // When & Then
            CryptographyException exception = assertThrows(CryptographyException.class, 
                () -> ((CryptographyServiceImpl) cryptographyService).decryptVote(encryptedVote, electionId));
            
            assertTrue(exception.getMessage().contains("Vote integrity verification failed"));
        }
    }
    
    @Nested
    @DisplayName("Hash Generation Tests")
    class HashGenerationTests {
        
        @Test
        @DisplayName("Should generate consistent hash for same data")
        void shouldGenerateConsistentHashForSameData() {
            // Given
            String data = "test-data-123";
            
            // When
            String hash1 = cryptographyService.generateHash(data);
            String hash2 = cryptographyService.generateHash(data);
            
            // Then
            assertNotNull(hash1);
            assertNotNull(hash2);
            assertEquals(hash1, hash2);
        }
        
        @Test
        @DisplayName("Should generate different hashes for different data")
        void shouldGenerateDifferentHashesForDifferentData() {
            // Given
            String data1 = "test-data-123";
            String data2 = "test-data-456";
            
            // When
            String hash1 = cryptographyService.generateHash(data1);
            String hash2 = cryptographyService.generateHash(data2);
            
            // Then
            assertNotEquals(hash1, hash2);
        }
        
        @Test
        @DisplayName("Should generate SHA-256 length hash")
        void shouldGenerateSHA256LengthHash() {
            // Given
            String data = "test-data";
            
            // When
            String hash = cryptographyService.generateHash(data);
            
            // Then
            // SHA-256 produces 64 character hex string
            assertEquals(64, hash.length());
        }
        
        @Test
        @DisplayName("Should throw exception for null data")
        void shouldThrowExceptionForNullData() {
            // When & Then
            CryptographyException exception = assertThrows(CryptographyException.class, 
                () -> cryptographyService.generateHash(null));
            
            assertTrue(exception.getMessage().contains("Data to hash cannot be null"));
        }
        
        @Test
        @DisplayName("Should handle empty string")
        void shouldHandleEmptyString() {
            // Given
            String data = "";
            
            // When
            String hash = cryptographyService.generateHash(data);
            
            // Then
            assertNotNull(hash);
            assertEquals(64, hash.length());
        }
    }
    
    @Nested
    @DisplayName("Hash Verification Tests")
    class HashVerificationTests {
        
        @Test
        @DisplayName("Should verify correct hash successfully")
        void shouldVerifyCorrectHashSuccessfully() {
            // Given
            String data = "test-verification-data";
            String hash = cryptographyService.generateHash(data);
            
            // When
            boolean isValid = cryptographyService.verifyHash(data, hash);
            
            // Then
            assertTrue(isValid);
        }
        
        @Test
        @DisplayName("Should reject incorrect hash")
        void shouldRejectIncorrectHash() {
            // Given
            String data = "test-verification-data";
            String wrongHash = "incorrect-hash-value";
            
            // When
            boolean isValid = cryptographyService.verifyHash(data, wrongHash);
            
            // Then
            assertFalse(isValid);
        }
        
        @Test
        @DisplayName("Should return false for null data")
        void shouldReturnFalseForNullData() {
            // Given
            String hash = "some-hash";
            
            // When
            boolean isValid = cryptographyService.verifyHash(null, hash);
            
            // Then
            assertFalse(isValid);
        }
        
        @Test
        @DisplayName("Should return false for null hash")
        void shouldReturnFalseForNullHash() {
            // Given
            String data = "some-data";
            
            // When
            boolean isValid = cryptographyService.verifyHash(data, null);
            
            // Then
            assertFalse(isValid);
        }
    }
    
    @Nested
    @DisplayName("Election Key Generation Tests")
    class ElectionKeyGenerationTests {
        
        @Test
        @DisplayName("Should generate election key successfully")
        void shouldGenerateElectionKeySuccessfully() {
            // Given
            String electionId = "election-123";
            
            // When
            byte[] key = cryptographyService.generateElectionKey(electionId);
            
            // Then
            assertNotNull(key);
            assertEquals(32, key.length); // 256 bits = 32 bytes
        }
        
        @Test
        @DisplayName("Should generate different keys for different elections")
        void shouldGenerateDifferentKeysForDifferentElections() {
            // Given
            String electionId1 = "election-123";
            String electionId2 = "election-456";
            
            // When
            byte[] key1 = cryptographyService.generateElectionKey(electionId1);
            byte[] key2 = cryptographyService.generateElectionKey(electionId2);
            
            // Then
            assertFalse(java.util.Arrays.equals(key1, key2));
        }
        
        @Test
        @DisplayName("Should throw exception for null election ID")
        void shouldThrowExceptionForNullElectionId() {
            // When & Then
            CryptographyException exception = assertThrows(CryptographyException.class, 
                () -> cryptographyService.generateElectionKey(null));
            
            assertTrue(exception.getMessage().contains("Election ID cannot be null or empty"));
        }
        
        @Test
        @DisplayName("Should throw exception for empty election ID")
        void shouldThrowExceptionForEmptyElectionId() {
            // When & Then
            CryptographyException exception = assertThrows(CryptographyException.class, 
                () -> cryptographyService.generateElectionKey(""));
            
            assertTrue(exception.getMessage().contains("Election ID cannot be null or empty"));
        }
    }
    
    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {
        
        @Test
        @DisplayName("Should handle complete encrypt-decrypt cycle")
        void shouldHandleCompleteEncryptDecryptCycle() {
            // Given
            String originalData = "candidate-integration-test";
            String electionId = "election-integration";
            
            // When
            EncryptedVote encrypted = cryptographyService.encryptVote(originalData, electionId);
            String decrypted = ((CryptographyServiceImpl) cryptographyService).decryptVote(encrypted, electionId);
            
            // Then
            assertEquals(originalData, decrypted);
        }
        
        @Test
        @DisplayName("Should handle multiple votes for same election")
        void shouldHandleMultipleVotesForSameElection() {
            // Given
            String electionId = "election-multi-vote";
            String vote1 = "candidate-1";
            String vote2 = "candidate-2";
            String vote3 = "candidate-3";
            
            // When
            EncryptedVote encrypted1 = cryptographyService.encryptVote(vote1, electionId);
            EncryptedVote encrypted2 = cryptographyService.encryptVote(vote2, electionId);
            EncryptedVote encrypted3 = cryptographyService.encryptVote(vote3, electionId);
            
            String decrypted1 = ((CryptographyServiceImpl) cryptographyService).decryptVote(encrypted1, electionId);
            String decrypted2 = ((CryptographyServiceImpl) cryptographyService).decryptVote(encrypted2, electionId);
            String decrypted3 = ((CryptographyServiceImpl) cryptographyService).decryptVote(encrypted3, electionId);
            
            // Then
            assertEquals(vote1, decrypted1);
            assertEquals(vote2, decrypted2);
            assertEquals(vote3, decrypted3);
        }
        
        @Test
        @DisplayName("Should handle unicode characters in vote data")
        void shouldHandleUnicodeCharactersInVoteData() {
            // Given
            String unicodeVote = "候選人-123-José-François-Müller";
            String electionId = "election-unicode";
            
            // When
            EncryptedVote encrypted = cryptographyService.encryptVote(unicodeVote, electionId);
            String decrypted = ((CryptographyServiceImpl) cryptographyService).decryptVote(encrypted, electionId);
            
            // Then
            assertEquals(unicodeVote, decrypted);
        }
    }
}