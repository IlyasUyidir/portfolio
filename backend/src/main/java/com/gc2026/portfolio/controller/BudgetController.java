package com.gc2026.portfolio.controller;

import com.gc2026.portfolio.dto.request.CreateBudgetRequest;
import com.gc2026.portfolio.dto.response.BudgetProgressResponse;
import com.gc2026.portfolio.dto.response.BudgetResponse;
import com.gc2026.portfolio.service.BudgetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
@Validated
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping
    public ResponseEntity<BudgetResponse> createOrUpdateBudget(
            HttpServletRequest httpRequest,
            @Valid @RequestBody CreateBudgetRequest request) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.status(HttpStatus.CREATED).body(budgetService.createOrUpdate(userId, request));
    }

    @GetMapping("/{month}")
    public ResponseEntity<List<BudgetProgressResponse>> getBudgetsByMonth(
            HttpServletRequest httpRequest,
            @PathVariable @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "Month must be in YYYY-MM format") String month) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        YearMonth yearMonth = YearMonth.parse(month);
        return ResponseEntity.ok(budgetService.getBudgetsByMonth(userId, yearMonth));
    }

    @GetMapping("/{id}/progress")
    public ResponseEntity<BudgetProgressResponse> getBudgetProgress(
            HttpServletRequest httpRequest,
            @PathVariable @Positive(message = "ID must be positive") Long id) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(budgetService.getProgress(userId, id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(
            HttpServletRequest httpRequest,
            @PathVariable @Positive(message = "ID must be positive") Long id) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        budgetService.deleteBudget(userId, id);
        return ResponseEntity.noContent().build();
    }
}
