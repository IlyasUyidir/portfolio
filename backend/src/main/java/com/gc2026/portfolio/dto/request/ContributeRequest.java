package com.gc2026.portfolio.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ContributeRequest {

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be strictly positive")
    private Long amount;
}
