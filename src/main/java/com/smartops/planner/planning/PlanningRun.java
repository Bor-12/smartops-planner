package com.smartops.planner.planning;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "planning_runs")
public class PlanningRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PlanningRunStatus status = PlanningRunStatus.RUNNING;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant finishedAt;

    @Column(nullable = false)
    private Integer totalTasks = 0;

    @Column(nullable = false)
    private Integer assignedTasks = 0;

    @Column(nullable = false)
    private Integer unassignedTasks = 0;

    @Column(columnDefinition = "TEXT")
    private String summary;

    protected PlanningRun() {
    }

    public PlanningRun(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public void complete(Integer totalTasks, Integer assignedTasks, Integer unassignedTasks, String summary, Instant finishedAt) {
        this.status = PlanningRunStatus.COMPLETED;
        this.totalTasks = totalTasks;
        this.assignedTasks = assignedTasks;
        this.unassignedTasks = unassignedTasks;
        this.summary = summary;
        this.finishedAt = finishedAt;
    }

    public Long getId() {
        return id;
    }

    public PlanningRunStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public Integer getTotalTasks() {
        return totalTasks;
    }

    public Integer getAssignedTasks() {
        return assignedTasks;
    }

    public Integer getUnassignedTasks() {
        return unassignedTasks;
    }

    public String getSummary() {
        return summary;
    }
}
