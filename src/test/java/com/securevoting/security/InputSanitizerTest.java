package com.securevoting.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InputSanitizerTest {

    private InputSanitizer inputSanitizer;

    @BeforeEach
    void setUp() {
        inputSanitizer = new InputSanitizer();
    }

    @Test
    void testSanitizeHtml_BasicText_ReturnsUnchanged() {
        // Given
        String input = "Hello World";

        // When
        String result = inputSanitizer.sanitizeHtml(input);

        // Then
        assertEquals("Hello World", result);
    }

    @Test
    void testSanitizeHtml_ScriptTag_RemovesScript() {
        // Given
        String input = "Hello <script>alert('xss')</script> World";

        // When
        String result = inputSanitizer.sanitizeHtml(input);

        // Then
        assertFalse(result.contains("<script>"));
        assertFalse(result.contains("alert"));
    }

    @Test
    void testSanitizeHtml_HtmlCharacters_EscapesCharacters() {
        // Given
        String input = "Hello <b>World</b> & Friends";

        // When
        String result = inputSanitizer.sanitizeHtml(input);

        // Then
        assertTrue(result.contains("&lt;b&gt;"));
        assertTrue(result.contains("&amp;"));
    }

    @Test
    void testSanitizeHtml_NullInput_ReturnsNull() {
        // When
        String result = inputSanitizer.sanitizeHtml(null);

        // Then
        assertNull(result);
    }

    @Test
    void testIsSqlSafe_NormalText_ReturnsTrue() {
        // Given
        String input = "John Doe";

        // When
        boolean result = inputSanitizer.isSqlSafe(input);

        // Then
        assertTrue(result);
    }

    @Test
    void testIsSqlSafe_SqlInjection_ReturnsFalse() {
        // Given
        String input = "admin' OR '1'='1";

        // When
        boolean result = inputSanitizer.isSqlSafe(input);

        // Then
        assertFalse(result);
    }

    @Test
    void testIsSqlSafe_UnionSelect_ReturnsFalse() {
        // Given
        String input = "test UNION SELECT * FROM users";

        // When
        boolean result = inputSanitizer.isSqlSafe(input);

        // Then
        assertFalse(result);
    }

    @Test
    void testIsSqlSafe_NullInput_ReturnsTrue() {
        // When
        boolean result = inputSanitizer.isSqlSafe(null);

        // Then
        assertTrue(result);
    }

    @Test
    void testIsPathSafe_NormalPath_ReturnsTrue() {
        // Given
        String input = "documents/file.txt";

        // When
        boolean result = inputSanitizer.isPathSafe(input);

        // Then
        assertTrue(result);
    }

    @Test
    void testIsPathSafe_PathTraversal_ReturnsFalse() {
        // Given
        String input = "../../../etc/passwd";

        // When
        boolean result = inputSanitizer.isPathSafe(input);

        // Then
        assertFalse(result);
    }

    @Test
    void testIsPathSafe_EncodedPathTraversal_ReturnsFalse() {
        // Given
        String input = "%2e%2e%2f%2e%2e%2fetc%2fpasswd";

        // When
        boolean result = inputSanitizer.isPathSafe(input);

        // Then
        assertFalse(result);
    }

    @Test
    void testSanitizeUserInput_ValidInput_ReturnsSanitized() {
        // Given
        String input = "Hello <b>World</b>";

        // When
        String result = inputSanitizer.sanitizeUserInput(input);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("&lt;b&gt;"));
    }

    @Test
    void testSanitizeUserInput_SqlInjection_ThrowsException() {
        // Given
        String input = "admin' OR '1'='1";

        // When & Then
        assertThrows(SecurityException.class, () -> {
            inputSanitizer.sanitizeUserInput(input);
        });
    }

    @Test
    void testSanitizeUserInput_PathTraversal_ThrowsException() {
        // Given
        String input = "../../../etc/passwd";

        // When & Then
        assertThrows(SecurityException.class, () -> {
            inputSanitizer.sanitizeUserInput(input);
        });
    }

    @Test
    void testSanitizeUserInput_TooLong_TruncatesInput() {
        // Given
        String input = "a".repeat(1500); // Longer than 1000 characters

        // When
        String result = inputSanitizer.sanitizeUserInput(input);

        // Then
        assertEquals(1000, result.length());
    }

    @Test
    void testSanitizeElectionInput_ValidInput_ReturnsSanitized() {
        // Given
        String input = "Presidential Election 2024";

        // When
        String result = inputSanitizer.sanitizeElectionInput(input);

        // Then
        assertEquals("Presidential Election 2024", result);
    }

    @Test
    void testSanitizeElectionInput_HtmlTags_RemovesTags() {
        // Given
        String input = "Election <b>2024</b> <script>alert('xss')</script>";

        // When
        String result = inputSanitizer.sanitizeElectionInput(input);

        // Then
        assertFalse(result.contains("<b>"));
        assertFalse(result.contains("<script>"));
        assertTrue(result.contains("Election"));
        assertTrue(result.contains("2024"));
    }

    @Test
    void testSanitizeElectionInput_ExtraWhitespace_NormalizesWhitespace() {
        // Given
        String input = "Election    2024\n\n   Test";

        // When
        String result = inputSanitizer.sanitizeElectionInput(input);

        // Then
        assertEquals("Election 2024 Test", result);
    }

    @Test
    void testIsValidUsername_ValidUsername_ReturnsTrue() {
        // Given
        String username = "john_doe123";

        // When
        boolean result = inputSanitizer.isValidUsername(username);

        // Then
        assertTrue(result);
    }

    @Test
    void testIsValidUsername_InvalidCharacters_ReturnsFalse() {
        // Given
        String username = "john@doe.com";

        // When
        boolean result = inputSanitizer.isValidUsername(username);

        // Then
        assertFalse(result);
    }

    @Test
    void testIsValidUsername_TooShort_ReturnsFalse() {
        // Given
        String username = "ab";

        // When
        boolean result = inputSanitizer.isValidUsername(username);

        // Then
        assertFalse(result);
    }

    @Test
    void testIsValidUsername_TooLong_ReturnsFalse() {
        // Given
        String username = "a".repeat(25);

        // When
        boolean result = inputSanitizer.isValidUsername(username);

        // Then
        assertFalse(result);
    }

    @Test
    void testIsValidUsername_NullOrEmpty_ReturnsFalse() {
        // When & Then
        assertFalse(inputSanitizer.isValidUsername(null));
        assertFalse(inputSanitizer.isValidUsername(""));
        assertFalse(inputSanitizer.isValidUsername("   "));
    }

    @Test
    void testIsValidEmail_ValidEmail_ReturnsTrue() {
        // Given
        String email = "john.doe@example.com";

        // When
        boolean result = inputSanitizer.isValidEmail(email);

        // Then
        assertTrue(result);
    }

    @Test
    void testIsValidEmail_InvalidFormat_ReturnsFalse() {
        // Given
        String email = "invalid-email";

        // When
        boolean result = inputSanitizer.isValidEmail(email);

        // Then
        assertFalse(result);
    }

    @Test
    void testIsValidEmail_TooLong_ReturnsFalse() {
        // Given
        String email = "a".repeat(95) + "@example.com"; // Over 100 characters

        // When
        boolean result = inputSanitizer.isValidEmail(email);

        // Then
        assertFalse(result);
    }

    @Test
    void testIsValidEmail_NullOrEmpty_ReturnsFalse() {
        // When & Then
        assertFalse(inputSanitizer.isValidEmail(null));
        assertFalse(inputSanitizer.isValidEmail(""));
        assertFalse(inputSanitizer.isValidEmail("   "));
    }

    @Test
    void testIsValidEmail_WithPlusSign_ReturnsTrue() {
        // Given
        String email = "john.doe+test@example.com";

        // When
        boolean result = inputSanitizer.isValidEmail(email);

        // Then
        assertTrue(result);
    }
}