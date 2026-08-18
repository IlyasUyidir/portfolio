package com.gc2026.portfolio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gc2026.portfolio.domain.exception.ResourceNotFoundException;
import com.gc2026.portfolio.dto.request.CreateBudgetRequest;
import com.gc2026.portfolio.dto.response.BudgetProgressResponse;
import com.gc2026.portfolio.dto.response.BudgetResponse;
import com.gc2026.portfolio.dto.response.CategoryResponse;
import com.gc2026.portfolio.security.JwtFilter;
import com.gc2026.portfolio.security.RateLimitFilter;
import com.gc2026.portfolio.service.BudgetService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.time.YearMonth;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BudgetController.class)
class BudgetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BudgetService budgetService;

    @MockitoBean
    private JwtFilter jwtFilter;

    @MockitoBean
    private RateLimitFilter rateLimitFilter;

    private BudgetResponse budgetResponse;
    private BudgetProgressResponse progressResponse;

    @BeforeEach
    void setUp() throws ServletException, IOException {
        // Mock filters to be transparent
        doAnswer(invocation -> {
            ((FilterChain) invocation.getArgument(2)).doFilter(
                    invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtFilter).doFilter(any(), any(), any());

        doAnswer(invocation -> {
            ((FilterChain) invocation.getArgument(2)).doFilter(
                    invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(rateLimitFilter).doFilter(any(), any(), any());

        CategoryResponse catResponse = CategoryResponse.builder()
                .id(10L)
                .name("Alimentation")
                .type("DEPENSE")
                .color("#EF4444")
                .isSystem(true)
                .build();

        budgetResponse = BudgetResponse.builder()
                .id(1L)
                .userId(1L)
                .category(catResponse)
                .budgetYear(2026)
                .budgetMonth(5)
                .limitAmount(200000L)
                .alertThreshold(80)
                .build();

        progressResponse = BudgetProgressResponse.builder()
                .budget(budgetResponse)
                .spentAmount(50000L)
                .remainingAmount(150000L)
                .spentPercentage(25)
                .alertStatus("NORMAL")
                .build();
    }

    // --- POST /api/v1/budgets ---

    @Test
    @WithMockUser
    @DisplayName("1. createOrUpdate_whenValidRequest_shouldReturn201")
    void createOrUpdate_whenValidRequest_shouldReturn201() throws Exception {
        // Arrange
        CreateBudgetRequest request = new CreateBudgetRequest();
        request.setCategoryId(10L);
        request.setBudgetYear(2026);
        request.setBudgetMonth(5);
        request.setLimitAmount(200000L);

        when(budgetService.createOrUpdate(eq(1L), any(CreateBudgetRequest.class))).thenReturn(budgetResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/budgets")
                        .with(csrf())
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.limitAmount").value(200000));
    }

    @Test
    @WithMockUser
    @DisplayName("2. createOrUpdate_whenMissingCategoryId_shouldReturn400")
    void createOrUpdate_whenMissingCategoryId_shouldReturn400() throws Exception {
        // Arrange
        CreateBudgetRequest request = new CreateBudgetRequest();
        // categoryId is null
        request.setBudgetYear(2026);
        request.setBudgetMonth(5);
        request.setLimitAmount(200000L);

        // Act & Assert
        mockMvc.perform(post("/api/v1/budgets")
                        .with(csrf())
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // --- GET /api/v1/budgets/{month} ---

    @Test
    @WithMockUser
    @DisplayName("3. getBudgetsByMonth_whenValidMonthFormat_shouldReturn200")
    void getBudgetsByMonth_whenValidMonthFormat_shouldReturn200() throws Exception {
        // Arrange
        when(budgetService.getBudgetsByMonth(eq(1L), any(YearMonth.class))).thenReturn(List.of(progressResponse));

        // Act & Assert
        mockMvc.perform(get("/api/v1/budgets/2026-05")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].spentAmount").value(50000));
    }

    @Test
    @WithMockUser
    @DisplayName("4. getBudgetsByMonth_whenInvalidMonthFormat_shouldReturn400")
    void getBudgetsByMonth_whenInvalidMonthFormat_shouldReturn400() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/budgets/invalid-month")
                        .requestAttr("userId", 1L))
                .andExpect(status().isBadRequest());
    }

    // --- GET /api/v1/budgets/{id}/progress ---

    @Test
    @WithMockUser
    @DisplayName("5. getBudgetProgress_whenFound_shouldReturn200")
    void getBudgetProgress_whenFound_shouldReturn200() throws Exception {
        // Arrange
        when(budgetService.getProgress(1L, 1L)).thenReturn(progressResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/budgets/1/progress")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spentPercentage").value(25));
    }

    @Test
    @WithMockUser
    @DisplayName("6. getBudgetProgress_whenNotFound_shouldReturn404")
    void getBudgetProgress_whenNotFound_shouldReturn404() throws Exception {
        // Arrange
        when(budgetService.getProgress(1L, 99L)).thenThrow(new ResourceNotFoundException("Budget not found"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/budgets/99/progress")
                        .requestAttr("userId", 1L))
                .andExpect(status().isNotFound());
    }

    // --- DELETE /api/v1/budgets/{id} ---

    @Test
    @WithMockUser
    @DisplayName("7. deleteBudget_whenFound_shouldReturn204")
    void deleteBudget_whenFound_shouldReturn204() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/v1/budgets/1")
                        .with(csrf())
                        .requestAttr("userId", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    @DisplayName("8. deleteBudget_whenNotFound_shouldReturn404")
    void deleteBudget_whenNotFound_shouldReturn404() throws Exception {
        // Arrange
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Budget not found"))
                .when(budgetService).deleteBudget(1L, 99L);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/budgets/99")
                        .with(csrf())
                        .requestAttr("userId", 1L))
                .andExpect(status().isNotFound());
    }

    // ─── I-4 regression: @Min(1) @Max(12) on budgetMonth ─────────────────────────

    /**
     * I-4 regression: The old code had a manual range check in BudgetService that only
     * fired AFTER database calls were already being set up (category lookup etc.).
     * The new code uses @Min(1) @Max(12) on CreateBudgetRequest.budgetMonth and rejects
     * the request at the DTO-binding layer (before any service method is called).
     *
     * These tests verify the 400 response comes from DTO validation, not from the service layer.
     */
    @Test
    @WithMockUser
    @DisplayName("I-4: createOrUpdate with budgetMonth=0 should return 400 (DTO validation)")
    void createOrUpdate_whenBudgetMonthZero_shouldReturn400() throws Exception {
        CreateBudgetRequest request = new CreateBudgetRequest();
        request.setCategoryId(10L);
        request.setBudgetYear(2026);
        request.setBudgetMonth(0);  // invalid: below @Min(1)
        request.setLimitAmount(200000L);

        mockMvc.perform(post("/api/v1/budgets")
                        .with(csrf())
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser
    @DisplayName("I-4: createOrUpdate with budgetMonth=13 should return 400 (DTO validation)")
    void createOrUpdate_whenBudgetMonthThirteen_shouldReturn400() throws Exception {
        CreateBudgetRequest request = new CreateBudgetRequest();
        request.setCategoryId(10L);
        request.setBudgetYear(2026);
        request.setBudgetMonth(13);  // invalid: above @Max(12)
        request.setLimitAmount(200000L);

        mockMvc.perform(post("/api/v1/budgets")
                        .with(csrf())
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("VALIDATION_ERROR"));
    }
}

