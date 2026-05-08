package com.gc2026.portfolio.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BudgetProgressResponse {
    private BudgetResponse budget;
    private Long spentAmount;
    private Long remainingAmount;
    private Integer spentPercentage;
    private String alertStatus;
}
