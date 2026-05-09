package com.gc2026.portfolio.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.time.LocalDate;

@Data
public class CreateGoalRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Target amount is required")
    @Positive(message = "Target amount must be strictly positive")
    private Long targetAmount;

    @NotNull(message = "Target date is required")
    private LocalDate targetDate;
}
