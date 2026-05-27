package com.smartops.planner.planning;

public record ScoreExplanation(
        ScoreReason reason,
        int points,
        String message
) {
}
