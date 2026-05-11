package com.gc2026.portfolio.security;

import com.gc2026.portfolio.domain.entity.RevokedToken;
import com.gc2026.portfolio.repository.RevokedTokenRepository;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistTest {

    @Mock
    private RevokedTokenRepository revokedTokenRepository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private TokenBlacklist tokenBlacklist;

    @Test
    void blacklist_whenValidToken_shouldSaveWithCorrectExpiry() {
        // Arrange
        String token = "test.jwt.token";
        // Date has millisecond precision, so we must truncate our expected Instant
        Instant expiry = Instant.now().plusSeconds(3600).truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        when(jwtUtil.extractExpiration(token)).thenReturn(Date.from(expiry));

        // Act
        tokenBlacklist.blacklist(token);

        // Assert
        ArgumentCaptor<RevokedToken> captor = ArgumentCaptor.forClass(RevokedToken.class);
        verify(revokedTokenRepository).save(captor.capture());

        RevokedToken saved = captor.getValue();
        assertThat(saved.getToken()).isEqualTo(token);
        assertThat(saved.getExpiryDate()).isEqualTo(expiry);
    }

    @Test
    void blacklist_whenJwtExtractionFails_shouldFallbackToDefaultTTL() {
        // Arrange
        String token = "malformed.token";
        when(jwtUtil.extractExpiration(anyString())).thenThrow(new JwtException("Invalid token"));

        // Act
        tokenBlacklist.blacklist(token);

        // Assert
        ArgumentCaptor<RevokedToken> captor = ArgumentCaptor.forClass(RevokedToken.class);
        verify(revokedTokenRepository).save(captor.capture());

        RevokedToken saved = captor.getValue();
        assertThat(saved.getToken()).isEqualTo(token);
        // Default TTL is 1 day (86400 seconds)
        assertThat(saved.getExpiryDate()).isCloseTo(Instant.now().plusSeconds(86400),
                within(5, java.time.temporal.ChronoUnit.SECONDS));
    }

    @Test
    void isBlacklisted_whenTokenExists_shouldReturnTrue() {
        // Arrange
        String token = "blacklisted.token";
        when(revokedTokenRepository.existsByToken(token)).thenReturn(true);

        // Act
        boolean result = tokenBlacklist.isBlacklisted(token);

        // Assert
        assertThat(result).isTrue();
        verify(revokedTokenRepository).existsByToken(token);
    }

    @Test
    void isBlacklisted_whenTokenDoesNotExist_shouldReturnFalse() {
        // Arrange
        String token = "new.token";
        when(revokedTokenRepository.existsByToken(token)).thenReturn(false);

        // Act
        boolean result = tokenBlacklist.isBlacklisted(token);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void cleanupExpiredTokens_shouldCallDeleteByExpiryDateBefore() {
        // Act
        tokenBlacklist.cleanupExpiredTokens();

        // Assert
        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
        verify(revokedTokenRepository).deleteByExpiryDateBefore(captor.capture());
        assertThat(captor.getValue()).isBeforeOrEqualTo(Instant.now());
    }
}
