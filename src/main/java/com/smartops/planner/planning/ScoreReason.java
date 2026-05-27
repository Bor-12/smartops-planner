package com.smartops.planner.planning;

public enum ScoreReason {
    MATCHING_SKILLS,
    MISSING_SKILLS,
    SENIORITY_MATCH,
    SENIORITY_GAP,
    LOW_WORKLOAD,
    HIGH_WORKLOAD,
    CAPACITY_AVAILABLE,
    CAPACITY_EXCEEDED,
    CRITICAL_TASK,
    NEAR_DEADLINE
}
