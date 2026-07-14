package com.gc2026.portfolio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gc2026.portfolio.domain.exception.ResourceNotFoundException;
import com.gc2026.portfolio.domain.exception.ValidationException;
import com.gc2026.portfolio.dto.request.ContributeRequest;
import com.gc2026.portfolio.dto.request.CreateGoalRequest;
import com.gc2026.portfolio.dto.response.GoalProgressResponse;
import com.gc2026.portfolio.dto.response.GoalResponse;
import com.gc2026.portfolio.dto.response.MilestonesDto;
import com.gc2026.portfolio.security.JwtFilter;
import com.gc2026.portfolio.security.RateLimitFilter;
import com.gc2026.portfolio.service.GoalService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GoalController.class)
class GoalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GoalService goalService;

    @MockitoBean
    private JwtFilter jwtFilter;

    @MockitoBean
    private RateLimitFilter rateLimitFilter;

    private GoalResponse goalResponse;
    private GoalProgressResponse progressResponse;

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

        goalResponse = GoalResponse.builder()
                .id(1L)
                .userId(1L)
                .title("Voiture")
                .targetAmount(500000L)
                .currentAmount(100000L)
                .targetDate(LocalDate.now().plusMonths(6))
                .status("EN_COURS")
                .createdAt(LocalDateTime.now())
                .build();

        MilestonesDto milestones = MilestonesDto.builder()
                .twentyFive(false)
                .fifty(false)
                .seventyFive(false)
                .hundred(false)
                .build();

        progressResponse = GoalProgressResponse.builder()
                .goal(goalResponse)
                .progressPercentage(20)
                .milestones(milestones)
                .build();
    }

    // --- POST /api/v1/goals ---

    @Test
    @WithMockUser
    @DisplayName("1. createGoal_whenValidRequest_shouldReturn201")
    void createGoal_whenValidRequest_shouldReturn201() throws Exception {
        // Arrange
        CreateGoalRequest request = new CreateGoalRequest();
        request.setTitle("Voiture");
        request.setTargetAmount(500000L);
        request.setTargetDate(LocalDate.now().plusMonths(6));

        when(goalService.createGoal(eq(1L), eq("STANDARD"), any(CreateGoalRequest.class))).thenReturn(goalResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/goals")
                        .with(csrf())
                        .requestAttr("userId", 1L)
                        .requestAttr("userRole", "STANDARD")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Voiture"));
    }

    @Test
    @WithMockUser
    @DisplayName("2. createGoal_whenTitleIsBlank_shouldReturn400")
    void createGoal_whenTitleIsBlank_shouldReturn400() throws Exception {
        // Arrange
        CreateGoalRequest request = new CreateGoalRequest();
        request.setTitle(""); // Blank title
        request.setTargetAmount(500000L);
        request.setTargetDate(LocalDate.now().plusMonths(6));

        // Act & Assert
        mockMvc.perform(post("/api/v1/goals")
                        .with(csrf())
                        .requestAttr("userId", 1L)
                        .requestAttr("userRole", "STANDARD")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("3. createGoal_whenTargetAmountIsNegative_shouldReturn400")
    void createGoal_whenTargetAmountIsNegative_shouldReturn400() throws Exception {
        // Arrange
        CreateGoalRequest request = new CreateGoalRequest();
        request.setTitle("Voiture");
        request.setTargetAmount(-1L); // Negative amount
        request.setTargetDate(LocalDate.now().plusMonths(6));

        // Act & Assert
        mockMvc.perform(post("/api/v1/goals")
                        .with(csrf())
                        .requestAttr("userId", 1L)
                        .requestAttr("userRole", "STANDARD")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("4. createGoal_whenStandardUserLimitReached_shouldReturn400")
    void createGoal_whenStandardUserLimitReached_shouldReturn400() throws Exception {
        // Arrange
        CreateGoalRequest request = new CreateGoalRequest();
        request.setTitle("Vacances");
        request.setTargetAmount(200000L);
        request.setTargetDate(LocalDate.now().plusMonths(2));

        when(goalService.createGoal(eq(1L), eq("STANDARD"), any(CreateGoalRequest.class)))
                .thenThrow(new ValidationException("Standard users can only have 1 active goal"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/goals")
                        .with(csrf())
                        .requestAttr("userId", 1L)
                        .requestAttr("userRole", "STANDARD")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Standard users can only have 1 active goal"));
    }

    // --- GET /api/v1/goals ---

    @Test
    @WithMockUser
    @DisplayName("5. getUserGoals_shouldReturn200WithList")
    void getUserGoals_shouldReturn200WithList() throws Exception {
        // Arrange
        when(goalService.getUserGoals(1L)).thenReturn(List.of(goalResponse));

        // Act & Assert
        mockMvc.perform(get("/api/v1/goals")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].title").value("Voiture"));
    }

    // --- POST /api/v1/goals/{id}/contribute ---

    @Test
    @WithMockUser
    @DisplayName("6. addContribution_whenValidRequest_shouldReturn200")
    void addContribution_whenValidRequest_shouldReturn200() throws Exception {
        // Arrange
        ContributeRequest request = new ContributeRequest();
        request.setAmount(50000L);

        when(goalService.addContribution(eq(1L), eq(1L), any(ContributeRequest.class))).thenReturn(goalResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/goals/1/contribute")
                        .with(csrf())
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @WithMockUser
    @DisplayName("7. addContribution_whenGoalAlreadyAchieved_shouldReturn400")
    void addContribution_whenGoalAlreadyAchieved_shouldReturn400() throws Exception {
        // Arrange
        ContributeRequest request = new ContributeRequest();
        request.setAmount(50000L);

        when(goalService.addContribution(eq(1L), eq(1L), any(ContributeRequest.class)))
                .thenThrow(new ValidationException("Cannot contribute to an already achieved goal"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/goals/1/contribute")
                        .with(csrf())
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cannot contribute to an already achieved goal"));
    }

    @Test
    @WithMockUser
    @DisplayName("8. addContribution_whenAmountIsZero_shouldReturn400")
    void addContribution_whenAmountIsZero_shouldReturn400() throws Exception {
        // Arrange
        ContributeRequest request = new ContributeRequest();
        request.setAmount(0L); // Fails @Positive

        // Act & Assert
        mockMvc.perform(post("/api/v1/goals/1/contribute")
                        .with(csrf())
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // --- GET /api/v1/goals/{id}/progress ---

    @Test
    @WithMockUser
    @DisplayName("9. getGoalProgress_whenFound_shouldReturn200")
    void getGoalProgress_whenFound_shouldReturn200() throws Exception {
        // Arrange
        when(goalService.getProgress(1L, 1L)).thenReturn(progressResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/goals/1/progress")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progressPercentage").value(20));
    }

    // --- DELETE /api/v1/goals/{id} ---

    @Test
    @WithMockUser
    @DisplayName("10. deleteGoal_whenFound_shouldReturn204")
    void deleteGoal_whenFound_shouldReturn204() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/v1/goals/1")
                        .with(csrf())
                        .requestAttr("userId", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    @DisplayName("11. deleteGoal_whenGoalBelongsToOtherUser_shouldReturn404")
    void deleteGoal_whenGoalBelongsToOtherUser_shouldReturn404() throws Exception {
        // Arrange
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Goal not found"))
                .when(goalService).deleteGoal(1L, 99L);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/goals/99")
                        .with(csrf())
                        .requestAttr("userId", 1L))
                .andExpect(status().isNotFound());
    }
}
