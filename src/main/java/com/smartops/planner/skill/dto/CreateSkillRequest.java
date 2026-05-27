package com.smartops.planner.skill.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSkillRequest(
        @NotBlank
        @Size(max = 100)
        String name
) {
}
