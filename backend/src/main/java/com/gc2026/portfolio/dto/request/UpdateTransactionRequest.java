package com.gc2026.portfolio.dto.request;

import com.gc2026.portfolio.domain.enums.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTransactionRequest {

    @NotBlank(message = "title is required")
    private String title;

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be positive")
    private Long amount; // in centimes

    @NotNull(message = "type is required")
    private TransactionType type;

    @NotNull(message = "categoryId is required")
    private Long categoryId;

    @NotNull(message = "txDate is required")
    private LocalDate txDate;

    private String description; // optional
}
