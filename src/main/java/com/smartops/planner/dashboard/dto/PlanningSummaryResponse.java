package com.smartops.planner.dashboard.dto;

import com.smartops.planner.planning.PlanningRunStatus;
import java.time.Instant;

public record PlanningSummaryResponse(
        long assignedTasks,
        long pendingTasks,
        long criticalPendingTasks,
        long totalAssignments,
        double averageAssignmentScore,
        Long latestPlanningRunId,
        PlanningRunStatus latestPlanningRunStatus,
        Instant latestPlanningRunFinishedAt
) {
}
