package com.smartops.planner.planning.dto;

import com.smartops.planner.planning.PlanningRunStatus;
import java.time.Instant;
import java.util.List;

public record PlanningRunResponse(
        Long id,
        PlanningRunStatus status,
        Instant startedAt,
        Instant finishedAt,
        Integer totalTasks,
        Integer assignedTasks,
        Integer unassignedTasks,
        String summary,
        List<AssignmentResponse> assignments
) {
}
