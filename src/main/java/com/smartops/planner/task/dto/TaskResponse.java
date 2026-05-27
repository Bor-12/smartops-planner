package com.smartops.planner.task.dto;

import com.smartops.planner.skill.dto.SkillResponse;
import com.smartops.planner.task.TaskPriority;
import com.smartops.planner.task.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskPriority priority,
        Integer estimatedHours,
        LocalDate deadline,
        TaskStatus status,
        List<SkillResponse> requiredSkills,
        Long assignedEmployeeId,
        Instant createdAt,
        Instant updatedAt
) {
}
