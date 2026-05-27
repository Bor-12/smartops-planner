package com.smartops.planner.dashboard.dto;

import com.smartops.planner.task.TaskStatus;

public record TaskStatusSummaryResponse(
        TaskStatus status,
        long count,
        double percentage
) {
}
