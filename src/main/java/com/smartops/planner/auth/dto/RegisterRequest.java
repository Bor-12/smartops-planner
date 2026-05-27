package com.smartops.planner.auth.dto;

import com.smartops.planner.user.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Size(max = 150)
        String username,

        @NotBlank
        @Size(min = 8, max = 100)
        String password,

        @NotNull
        Role role
) {
}
