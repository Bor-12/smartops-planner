package com.smartops.planner.task.dto;

import com.smartops.planner.task.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Set;

public record CreateTaskRequest(
        @NotBlank
        @Size(max = 200)
        String title,

        String description,

        @NotNull
        TaskPriority priority,

        @NotNull
        @Positive
        Integer estimatedHours,

        @NotNull
        LocalDate deadline,

        Set<@NotNull Long> requiredSkillIds
) {
}
