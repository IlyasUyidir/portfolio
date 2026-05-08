package com.gc2026.portfolio.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenBlacklist {

    private final ConcurrentHashMap<String, Instant> blacklistedTokens = new ConcurrentHashMap<>();

    public void blacklist(String token) {
        blacklistedTokens.put(token, Instant.now());
    }

    public boolean isBlacklisted(String token) {
        return blacklistedTokens.containsKey(token);
    }
}
