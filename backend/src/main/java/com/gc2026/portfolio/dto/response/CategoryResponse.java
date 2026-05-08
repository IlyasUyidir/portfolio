package com.gc2026.portfolio.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {

    private Long id;
    private String name;
    private String color;
    private String type; // "REVENU" | "DEPENSE" | "BOTH"
    private Boolean isSystem;
}
