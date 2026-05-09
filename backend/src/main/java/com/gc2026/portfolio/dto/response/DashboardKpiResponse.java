package com.gc2026.portfolio.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardKpiResponse {
    private Long totalIncome;
    private Long totalExpenses;
    private Long monthlyBalance;
    private Double savingsRate;
}
