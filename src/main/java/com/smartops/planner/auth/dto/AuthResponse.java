package com.smartops.planner.auth.dto;

import com.smartops.planner.user.Role;

public record AuthResponse(
        Long id,
        String username,
        Role role,
        String token
) {
}
