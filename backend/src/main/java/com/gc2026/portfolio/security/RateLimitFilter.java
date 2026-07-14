package com.gc2026.portfolio.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> authBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> exportBuckets = new ConcurrentHashMap<>();

    private Bucket createAuthBucket() {
        return Bucket.builder()
            .addLimit(Bandwidth.classic(
                10,                        // 10 attempts
                Refill.intervally(10, Duration.ofMinutes(1)) // per minute
            ))
            .build();
    }

    private Bucket createExportBucket() {
        return Bucket.builder()
            .addLimit(Bandwidth.classic(
                5,                         // 5 attempts
                Refill.intervally(5, Duration.ofMinutes(1)) // per minute
            ))
            .build();
    }

    private String extractIp(HttpServletRequest request) {
        // X-Real-IP is set by Nginx to the actual client IP — prefer it.
        // X-Forwarded-For in this stack carries the Nginx container's Docker
        // bridge IP as the first entry, which would collapse all users into
        // one shared bucket.
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp.trim();
        }
        // Fallback: Caddy or other upstream proxies may only set XFF.
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        
        boolean isAuth = path.startsWith("/api/v1/auth/");
        boolean isExport = path.startsWith("/api/v1/export/csv");

        if (!isAuth && !isExport) {
            chain.doFilter(request, response);
            return;
        }

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        // GET /api/v1/auth/me is a session-check, not a credential submission.
        // Rate-limiting it drains the auth bucket on every page load without
        // providing any brute-force protection — exclude it.
        if ("GET".equalsIgnoreCase(request.getMethod()) && path.equals("/api/v1/auth/me")) {
            chain.doFilter(request, response);
            return;
        }

        String ip = extractIp(request);
        Bucket bucket;

        if (isAuth) {
            bucket = authBuckets.computeIfAbsent(ip, k -> createAuthBucket());
        } else {
            bucket = exportBuckets.computeIfAbsent(ip, k -> createExportBucket());
        }

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(429); // Too Many Requests
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many requests. Try again in a minute.\"}");
        }
    }
}
