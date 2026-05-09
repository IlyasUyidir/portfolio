package com.gc2026.portfolio.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategorySpendingResponse {
    private String categoryName;
    private String color;
    private Long amount;
}
