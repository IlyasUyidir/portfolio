package com.gc2026.portfolio.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class GoalResponse {
    private Long id;
    private Long userId;
    private String title;
    private Long targetAmount;
    private Long currentAmount;
    private LocalDate targetDate;
    private String status;
    private LocalDateTime createdAt;
}
