package com.smartops.planner.task.dto;

import com.smartops.planner.task.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTaskStatusRequest(
        @NotNull
        TaskStatus status
) {
}
