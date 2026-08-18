package com.gc2026.portfolio.controller;

import com.gc2026.portfolio.config.SecurityConfig;
import com.gc2026.portfolio.security.JwtFilter;
import com.gc2026.portfolio.security.JwtUtil;
import com.gc2026.portfolio.security.RateLimitFilter;
import com.gc2026.portfolio.security.TokenBlacklist;
import com.gc2026.portfolio.service.GoalService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GoalController.class)
@Import({SecurityConfig.class, JwtFilter.class})
class GoalControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GoalService goalService;

    @MockitoBean
    private RateLimitFilter rateLimitFilter;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private TokenBlacklist tokenBlacklist;

    @BeforeEach
    void setUp() throws ServletException, IOException {
        doAnswer(invocation -> {
            ((FilterChain) invocation.getArgument(2)).doFilter(
                    invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(rateLimitFilter).doFilter(any(), any(), any());

    }

    @Test
    @DisplayName("S1. request_withoutCookie_shouldReturn401")
    void request_withoutCookie_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/goals"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("S2. request_withInvalidToken_shouldReturn401")
    void request_withInvalidToken_shouldReturn401() throws Exception {
        when(jwtUtil.isTokenValid("invalid.token")).thenReturn(false);

        mockMvc.perform(get("/api/v1/goals")
                        .cookie(new Cookie("auth_token", "invalid.token")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("S3. request_withValidToken_shouldReturn200")
    void request_withValidToken_shouldReturn200() throws Exception {
        when(jwtUtil.isTokenValid("valid.token")).thenReturn(true);
        when(jwtUtil.extractEmail("valid.token")).thenReturn("test@folio.io");
        when(jwtUtil.extractUserId("valid.token")).thenReturn(1L);
        when(jwtUtil.extractRole("valid.token")).thenReturn("STANDARD");
        when(tokenBlacklist.isBlacklisted("valid.token")).thenReturn(false);

        mockMvc.perform(get("/api/v1/goals")
                        .cookie(new Cookie("auth_token", "valid.token")))
                .andExpect(status().isOk());
    }
}
