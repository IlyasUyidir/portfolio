package com.gc2026.portfolio.dto.request;

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
    private Integer budgetMonth;

    @NotNull(message = "Limit amount is required")
    @Positive(message = "Limit amount must be strictly positive")
    private Long limitAmount;
}
