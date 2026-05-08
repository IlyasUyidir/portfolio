package com.gc2026.portfolio.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class BudgetResponse {
    private Long id;
    private Long userId;
    private CategoryResponse category;
    private Integer budgetYear;
    private Integer budgetMonth;
    private Long limitAmount;
    private Integer alertThreshold;
    private LocalDateTime createdAt;
}
