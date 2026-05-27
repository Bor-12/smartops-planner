package com.smartops.planner.common.response;

public record ApiError(
        int status,
        String error,
        String message,
        String path
) {
}
