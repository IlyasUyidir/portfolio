package com.gc2026.portfolio.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CreateBudgetRequest {

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotNull(message = "Budget year is required")
    private Integer budgetYear;

    @NotNull(message = "Budget month is required")
    @Min(value = 1, message = "Budget month must be between 1 and 12")
    @Max(value = 12, message = "Budget month must be between 1 and 12")
    private Integer budgetMonth;

    @NotNull(message = "Limit amount is required")
    @Positive(message = "Limit amount must be strictly positive")
    private Long limitAmount;

    private Integer alertThreshold;
}
