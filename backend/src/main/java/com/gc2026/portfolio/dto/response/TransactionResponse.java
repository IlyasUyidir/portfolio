package com.gc2026.portfolio.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private Long id;
    private String title;
    private Long amount; // in centimes
    private String type; // "REVENU" | "DEPENSE"
    private Long categoryId;
    private String categoryName;
    private LocalDate txDate;
    private String description;
    private LocalDateTime createdAt;
}
