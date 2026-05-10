package com.gc2026.portfolio.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class GoalProgressResponse {
    private GoalResponse goal;
    private Integer progressPercentage;
    private MilestonesDto milestones;
}
