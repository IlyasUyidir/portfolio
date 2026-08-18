package com.gc2026.portfolio.controller;

import com.gc2026.portfolio.config.SecurityConfig;
import com.gc2026.portfolio.security.JwtFilter;
import com.gc2026.portfolio.security.JwtUtil;
import com.gc2026.portfolio.security.RateLimitFilter;
import com.gc2026.portfolio.security.TokenBlacklist;
import com.gc2026.portfolio.service.ExportService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExportController.class)
@Import({SecurityConfig.class, JwtFilter.class})
class ExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExportService exportService;

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

    private void mockValidAuth() {
        when(jwtUtil.isTokenValid("valid.token")).thenReturn(true);
        when(jwtUtil.extractEmail("valid.token")).thenReturn("test@folio.io");
        when(jwtUtil.extractUserId("valid.token")).thenReturn(1L);
        when(jwtUtil.extractRole("valid.token")).thenReturn("STANDARD");
        when(tokenBlacklist.isBlacklisted("valid.token")).thenReturn(false);
    }

    // --- SECURITY FILTER TESTS ---

    @Test
    @DisplayName("Security: request without cookie should return 401")
    void request_withoutCookie_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/export/csv"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Security: request with invalid token should return 401")
    void request_withInvalidToken_shouldReturn401() throws Exception {
        when(jwtUtil.isTokenValid("invalid.token")).thenReturn(false);

        mockMvc.perform(get("/api/v1/export/csv")
                        .cookie(new Cookie("auth_token", "invalid.token")))
                .andExpect(status().isUnauthorized());
    }

    // --- EXPORT CSV TESTS ---

    @Test
    @DisplayName("GET /csv - should return correct headers and CSV body")
    void exportToCsv_shouldReturnCsvFile() throws Exception {
        mockValidAuth();
        
        String csvContent = "Date,Title,Amount\n2026-05-10,Courses,-50\n";
        when(exportService.exportToCsv(any())).thenReturn(csvContent);

        mockMvc.perform(get("/api/v1/export/csv")
                        .cookie(new Cookie("auth_token", "valid.token")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"))
                .andExpect(header().string("Content-Disposition", "form-data; name=\"attachment\"; filename=\"transactions.csv\""))
                .andExpect(content().string(csvContent));
    }
}
