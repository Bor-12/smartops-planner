package com.smartops.planner.planning;

import com.smartops.planner.employee.Employee;
import com.smartops.planner.task.Task;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "assignments")
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "planning_run_id", nullable = false)
    private PlanningRun planningRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(nullable = false)
    private Integer score;

    @Column(nullable = false)
    private Boolean assigned;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String explanation;

    @Column(nullable = false)
    private Instant createdAt;

    protected Assignment() {
    }

    public Assignment(
            PlanningRun planningRun,
            Task task,
            Employee employee,
            Integer score,
            Boolean assigned,
            String explanation,
            Instant createdAt
    ) {
        this.planningRun = planningRun;
        this.task = task;
        this.employee = employee;
        this.score = score;
        this.assigned = assigned;
        this.explanation = explanation;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public PlanningRun getPlanningRun() {
        return planningRun;
    }

    public Task getTask() {
        return task;
    }

    public Employee getEmployee() {
        return employee;
    }

    public Integer getScore() {
        return score;
    }

    public Boolean getAssigned() {
        return assigned;
    }

    public String getExplanation() {
        return explanation;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
