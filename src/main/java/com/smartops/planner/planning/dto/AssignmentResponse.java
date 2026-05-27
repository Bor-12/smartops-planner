package com.smartops.planner.planning.dto;

import java.time.Instant;

public record AssignmentResponse(
        Long id,
        Long planningRunId,
        Long taskId,
        String taskTitle,
        Long employeeId,
        String employeeName,
        Integer score,
        Boolean assigned,
        String explanation,
        Instant createdAt
) {
}
