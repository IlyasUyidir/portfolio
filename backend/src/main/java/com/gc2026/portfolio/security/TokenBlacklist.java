package com.gc2026.portfolio.security;

import com.gc2026.portfolio.domain.entity.RevokedToken;
import com.gc2026.portfolio.repository.RevokedTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklist {

    private final RevokedTokenRepository revokedTokenRepository;
    private final JwtUtil jwtUtil;

    public void blacklist(String token) {
        try {
            Instant expiryDate = jwtUtil.extractExpiration(token).toInstant();
            RevokedToken revokedToken = RevokedToken.builder()
                    .token(token)
                    .expiryDate(expiryDate)
                    .build();
            revokedTokenRepository.save(revokedToken);
            log.info("Token blacklisted until {}", expiryDate);
        } catch (Exception e) {
            log.warn("Failed to extract expiration from token, blacklisting with default TTL", e);
            // Fallback if token is malformed but we still want to blacklist it
            revokedTokenRepository.save(RevokedToken.builder()
                    .token(token)
                    .expiryDate(Instant.now().plusSeconds(86400)) // 1 day default
                    .build());
        }
    }

    public boolean isBlacklisted(String token) {
        return revokedTokenRepository.existsByToken(token);
    }

    @Scheduled(cron = "0 0 3 * * *") // Every day at 3 AM
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Starting cleanup of expired revoked tokens...");
        revokedTokenRepository.deleteByExpiryDateBefore(Instant.now());
        log.info("Cleanup completed.");
    }
}
