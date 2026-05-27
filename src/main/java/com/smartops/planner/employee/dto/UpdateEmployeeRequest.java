package com.smartops.planner.employee.dto;

import com.smartops.planner.employee.SeniorityLevel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record UpdateEmployeeRequest(
        @NotBlank
        @Size(max = 150)
        String name,

        @NotBlank
        @Email
        @Size(max = 150)
        String email,

        @NotNull
        @Positive
        Integer maxWeeklyHours,

        @NotNull
        @PositiveOrZero
        Integer currentWeeklyHours,

        @NotNull
        SeniorityLevel seniorityLevel,

        Set<@NotNull Long> skillIds
) {
}
