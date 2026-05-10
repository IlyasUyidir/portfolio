package com.gc2026.portfolio.security;

import com.gc2026.portfolio.domain.entity.RevokedToken;
import com.gc2026.portfolio.repository.RevokedTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
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
    void blacklist_savesTokenWithCorrectExpiry() {
        // Arrange
        String token = "test.jwt.token";
        Instant expiry = Instant.now().plusSeconds(3600).truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        when(jwtUtil.extractExpiration(token)).thenReturn(Date.from(expiry));

        // Act
        tokenBlacklist.blacklist(token);

        // Assert
        ArgumentCaptor<RevokedToken> captor = ArgumentCaptor.forClass(RevokedToken.class);
        verify(revokedTokenRepository).save(captor.capture());
        
        RevokedToken saved = captor.getValue();
        assertTrue(saved.getToken().equals(token));
        assertTrue(saved.getExpiryDate().equals(expiry));
    }

    @Test
    void isBlacklisted_checksRepository() {
        // Arrange
        String token = "test.jwt.token";
        when(revokedTokenRepository.existsByToken(token)).thenReturn(true);

        // Act
        boolean result = tokenBlacklist.isBlacklisted(token);

        // Assert
        assertTrue(result);
        verify(revokedTokenRepository).existsByToken(token);
    }

    @Test
    void cleanupExpiredTokens_callsRepositoryDelete() {
        // Act
        tokenBlacklist.cleanupExpiredTokens();

        // Assert
        verify(revokedTokenRepository).deleteByExpiryDateBefore(any(Instant.class));
    }
}
