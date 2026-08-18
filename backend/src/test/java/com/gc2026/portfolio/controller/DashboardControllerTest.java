package com.gc2026.portfolio.controller;

import com.gc2026.portfolio.config.SecurityConfig;
import com.gc2026.portfolio.dto.response.CategorySpendingResponse;
import com.gc2026.portfolio.dto.response.DashboardKpiResponse;
import com.gc2026.portfolio.dto.response.CategoryResponse;

import com.gc2026.portfolio.security.JwtFilter;
import com.gc2026.portfolio.security.JwtUtil;
import com.gc2026.portfolio.security.RateLimitFilter;
import com.gc2026.portfolio.security.TokenBlacklist;
import com.gc2026.portfolio.service.DashboardService;
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
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@Import({SecurityConfig.class, JwtFilter.class})
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

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
        mockMvc.perform(get("/api/v1/dashboard/kpis"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Security: request with invalid token should return 401")
    void request_withInvalidToken_shouldReturn401() throws Exception {
        when(jwtUtil.isTokenValid("invalid.token")).thenReturn(false);

        mockMvc.perform(get("/api/v1/dashboard/kpis")
                        .cookie(new Cookie("auth_token", "invalid.token")))
                .andExpect(status().isUnauthorized());
    }

    // --- DASHBOARD KPIS TESTS ---

    @Test
    @DisplayName("GET /kpis - valid month should return 200")
    void getKpis_withValidMonth_shouldReturn200() throws Exception {
        mockValidAuth();
        DashboardKpiResponse response = new DashboardKpiResponse();
        response.setTotalIncome(5000L);
        when(dashboardService.getKpis(any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/dashboard/kpis")
                        .param("month", "2026-05")
                        .cookie(new Cookie("auth_token", "valid.token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(5000));
    }

    @Test
    @DisplayName("GET /kpis - invalid month format should return 400 Bad Request")
    void getKpis_withInvalidMonth_shouldReturn400() throws Exception {
        mockValidAuth();

        mockMvc.perform(get("/api/v1/dashboard/kpis")
                        .param("month", "2026-13-99") // Invalid YearMonth
                        .cookie(new Cookie("auth_token", "valid.token")))
                .andExpect(status().isBadRequest());
    }

    // --- DASHBOARD SPENDING TESTS ---

    @Test
    @DisplayName("GET /spending - valid month should return 200 with list")
    void getSpending_withValidMonth_shouldReturn200() throws Exception {
        mockValidAuth();
        CategoryResponse catResp = CategoryResponse.builder().name("Alimentation").build();
        CategorySpendingResponse item = new CategorySpendingResponse();
        item.setCategory(catResp);
        item.setTotalAmount(1000L);
        when(dashboardService.getSpending(any(), any())).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/dashboard/spending")
                        .param("month", "2026-05")
                        .cookie(new Cookie("auth_token", "valid.token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category.name").value("Alimentation"))
                .andExpect(jsonPath("$[0].totalAmount").value(1000));
    }

    @Test
    @DisplayName("GET /spending - valid month with empty result should return 200 with empty list")
    void getSpending_withEmptyResult_shouldReturn200EmptyList() throws Exception {
        mockValidAuth();
        when(dashboardService.getSpending(any(), any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/dashboard/spending")
                        .param("month", "2026-05")
                        .cookie(new Cookie("auth_token", "valid.token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
