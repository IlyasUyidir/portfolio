package com.gc2026.portfolio.controller;

import com.gc2026.portfolio.dto.response.CategorySpendingResponse;
import com.gc2026.portfolio.dto.response.DashboardKpiResponse;
import com.gc2026.portfolio.service.DashboardService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/kpis")
    public ResponseEntity<DashboardKpiResponse> getKpis(
            HttpServletRequest httpRequest,
            @RequestParam(required = false) YearMonth month) {
        if (month == null) {
            month = YearMonth.now();
        }
        Long userId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(dashboardService.getKpis(userId, month));
    }

    @GetMapping("/spending")
    public ResponseEntity<List<CategorySpendingResponse>> getSpending(
            HttpServletRequest httpRequest,
            @RequestParam(required = false) YearMonth month) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(dashboardService.getSpending(userId, month));
    }
}
