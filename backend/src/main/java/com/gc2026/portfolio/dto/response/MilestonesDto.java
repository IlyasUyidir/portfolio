package com.gc2026.portfolio.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MilestonesDto {
    private boolean twentyFive;
    private boolean fifty;
    private boolean seventyFive;
    private boolean hundred;
}
