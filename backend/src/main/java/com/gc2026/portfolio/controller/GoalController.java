package com.gc2026.portfolio.controller;

import com.gc2026.portfolio.dto.request.ContributeRequest;
import com.gc2026.portfolio.dto.request.CreateGoalRequest;
import com.gc2026.portfolio.dto.response.GoalProgressResponse;
import com.gc2026.portfolio.dto.response.GoalResponse;
import com.gc2026.portfolio.service.GoalService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api/v1/goals")
@RequiredArgsConstructor
@Validated
public class GoalController {

    private final GoalService goalService;

    @PostMapping
    public ResponseEntity<GoalResponse> createGoal(
            HttpServletRequest httpRequest,
            @Valid @RequestBody CreateGoalRequest request) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        String userRole = (String) httpRequest.getAttribute("userRole");
        return ResponseEntity.status(HttpStatus.CREATED).body(goalService.createGoal(userId, userRole, request));
    }

    @GetMapping
    public ResponseEntity<List<GoalResponse>> getUserGoals(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(goalService.getUserGoals(userId));
    }

    @PostMapping("/{id}/contribute")
    public ResponseEntity<GoalResponse> addContribution(
            HttpServletRequest httpRequest,
            @PathVariable @Positive(message = "ID must be positive") Long id,
            @Valid @RequestBody ContributeRequest request) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(goalService.addContribution(userId, id, request));
    }

    @GetMapping("/{id}/progress")
    public ResponseEntity<GoalProgressResponse> getGoalProgress(
            HttpServletRequest httpRequest,
            @PathVariable @Positive(message = "ID must be positive") Long id) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(goalService.getProgress(userId, id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoal(
            HttpServletRequest httpRequest,
            @PathVariable @Positive(message = "ID must be positive") Long id) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        goalService.deleteGoal(userId, id);
        return ResponseEntity.noContent().build();
    }
}
