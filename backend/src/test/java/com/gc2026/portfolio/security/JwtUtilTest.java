package com.gc2026.portfolio.security;

import com.gc2026.portfolio.domain.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtUtilTest {

    private static final String TEST_SECRET = "this-is-a-32-char-secret-for-testing!!";
    private static final long EXPIRATION_MS = 86400000L; // 1 day
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(TEST_SECRET, EXPIRATION_MS);
    }

    // --- CONSTRUCTOR ---

    @Test
    void constructor_whenSecretIsNull_shouldThrowIllegalStateException() {
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> new JwtUtil(null, EXPIRATION_MS));
    }

    @Test
    void constructor_whenSecretIsBlank_shouldThrowIllegalStateException() {
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> new JwtUtil("   ", EXPIRATION_MS));
    }

    @Test
    void constructor_whenSecretIsTooShort_shouldThrowIllegalStateException() {
        // Arrange
        String shortSecret = "1234567890123456789012345678901"; // 31 chars

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> new JwtUtil(shortSecret, EXPIRATION_MS));
    }

    @Test
    void constructor_whenSecretIsExactly32Chars_shouldNotThrow() {
        // Arrange
        String exactSecret = "12345678901234567890123456789012"; // 32 chars

        // Act & Assert
        assertDoesNotThrow(() -> new JwtUtil(exactSecret, EXPIRATION_MS));
    }

    // --- TOKEN GENERATION & EXTRACTION ---

    @Test
    void generateToken_shouldReturnNonNullToken() {
        // Act
        String token = jwtUtil.generateToken(1L, "test@folio.io", UserRole.STANDARD);

        // Assert
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void generateToken_shouldProduceJwtWithThreeParts() {
        // Act
        String token = jwtUtil.generateToken(1L, "test@folio.io", UserRole.STANDARD);
        String[] parts = token.split("\\.");

        // Assert
        assertThat(parts.length).isEqualTo(3);
    }

    @Test
    void extractEmail_shouldReturnCorrectEmail() {
        // Arrange
        String email = "test@folio.io";
        String token = jwtUtil.generateToken(1L, email, UserRole.STANDARD);

        // Act
        String extractedEmail = jwtUtil.extractEmail(token);

        // Assert
        assertThat(extractedEmail).isEqualTo(email);
    }

    @Test
    void extractUserId_shouldReturnCorrectUserId() {
        // Arrange
        Long userId = 1L;
        String token = jwtUtil.generateToken(userId, "test@folio.io", UserRole.STANDARD);

        // Act
        Long extractedUserId = jwtUtil.extractUserId(token);

        // Assert
        assertThat(extractedUserId).isEqualTo(userId);
    }

    @Test
    void extractRole_shouldReturnCorrectRole() {
        // Arrange
        UserRole role = UserRole.STANDARD;
        String token = jwtUtil.generateToken(1L, "test@folio.io", role);

        // Act
        String extractedRole = jwtUtil.extractRole(token);

        // Assert
        assertThat(extractedRole).isEqualTo(role.name());
    }

    @Test
    void extractExpiration_shouldReturnFutureDate() {
        // Arrange
        String token = jwtUtil.generateToken(1L, "test@folio.io", UserRole.STANDARD);

        // Act
        Date expiry = jwtUtil.extractExpiration(token);

        // Assert
        assertThat(expiry).isAfter(new Date());
    }

    // --- VALIDITY & SECURITY ---

    @Test
    void isTokenValid_whenTokenIsValid_shouldReturnTrue() {
        // Arrange
        String token = jwtUtil.generateToken(1L, "test@folio.io", UserRole.STANDARD);

        // Act & Assert
        assertThat(jwtUtil.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_whenTokenIsExpired_shouldReturnFalse() {
        // Arrange
        JwtUtil expiredUtil = new JwtUtil(TEST_SECRET, -1000L); // Already expired
        String token = expiredUtil.generateToken(1L, "test@folio.io", UserRole.STANDARD);

        // Act & Assert
        assertThat(jwtUtil.isTokenValid(token)).isFalse();
    }

    @Test
    void isTokenValid_whenTokenIsTampered_shouldReturnFalse() {
        // Arrange
        String token = jwtUtil.generateToken(1L, "test@folio.io", UserRole.STANDARD);
        String tamperedToken = token.substring(0, token.length() - 5) + "XXXXX";

        // Act & Assert
        assertThat(jwtUtil.isTokenValid(tamperedToken)).isFalse();
    }

    @Test
    void isTokenValid_whenTokenIsMalformed_shouldReturnFalse() {
        // Arrange
        String malformedToken = "not.a.jwt.atall";

        // Act & Assert
        assertThat(jwtUtil.isTokenValid(malformedToken)).isFalse();
    }

    @Test
    void isTokenValid_whenTokenSignedWithDifferentSecret_shouldReturnFalse() {
        // Arrange
        JwtUtil otherUtil = new JwtUtil("different-secret-key-that-is-32-chars!!", EXPIRATION_MS);
        String foreignToken = otherUtil.generateToken(1L, "test@folio.io", UserRole.STANDARD);

        // Act & Assert
        assertThat(jwtUtil.isTokenValid(foreignToken)).isFalse();
    }

    @Test
    void isTokenValid_whenTokenIsNull_shouldReturnFalse() {
        // Act & Assert
        assertThat(jwtUtil.isTokenValid(null)).isFalse();
    }

    @Test
    void isTokenValid_whenTokenIsBlank_shouldReturnFalse() {
        // Act & Assert
        assertThat(jwtUtil.isTokenValid("   ")).isFalse();
    }

    @Test
    void jwtToken_shouldNotContainSensitiveData() {
        // Arrange
        String token = jwtUtil.generateToken(1L, "test@folio.io", UserRole.STANDARD);
        
        // Act
        String[] parts = token.split("\\.");
        String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));

        // Assert
        assertThat(payload).doesNotContain("password");
        assertThat(payload).doesNotContain("hash");
    }

    @Test
    void jwtFilter_shouldExtractUserIdAsLong_notInteger() {
        // Arrange
        String token = jwtUtil.generateToken(1L, "test@folio.io", UserRole.STANDARD);

        // Act
        Object extractedUserId = jwtUtil.extractUserId(token);

        // Assert
        assertThat(extractedUserId).isInstanceOf(Long.class);
        assertThat(extractedUserId).isEqualTo(1L);
    }
}
