package com.gc2026.portfolio.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardKpiResponse {
    private Long totalIncome;
    private Long totalExpenses;
    private Long monthlyBalance;
    private Double savingsRate;
}