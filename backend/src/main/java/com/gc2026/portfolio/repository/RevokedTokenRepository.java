package com.gc2026.portfolio.repository;

import com.gc2026.portfolio.domain.entity.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {
    boolean existsByToken(String token);
    void deleteByExpiryDateBefore(Instant now);
}
