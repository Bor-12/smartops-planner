package com.smartops.planner.dashboard.dto;

public record WorkloadResponse(
        Long employeeId,
        String employeeName,
        Integer currentWeeklyHours,
        Integer maxWeeklyHours,
        Integer remainingWeeklyHours,
        double workloadPercentage
) {
}
