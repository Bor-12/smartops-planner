package com.smartops.planner.user.dto;

import com.smartops.planner.user.Role;

public record UserResponse(
        Long id,
        String username,
        Role role
) {
}
