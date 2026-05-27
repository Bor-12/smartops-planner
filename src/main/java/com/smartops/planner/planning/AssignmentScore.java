package com.smartops.planner.planning;

import java.util.List;

public record AssignmentScore(
        int score,
        boolean eligible,
        String explanation,
        List<ScoreExplanation> positiveReasons,
        List<ScoreExplanation> penalties
) {
}
