CREATE TABLE planning_runs (
    id BIGSERIAL PRIMARY KEY,
    status VARCHAR(50) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    finished_at TIMESTAMP WITH TIME ZONE,
    total_tasks INTEGER NOT NULL DEFAULT 0,
    assigned_tasks INTEGER NOT NULL DEFAULT 0,
    unassigned_tasks INTEGER NOT NULL DEFAULT 0,
    summary TEXT
);

CREATE TABLE assignments (
    id BIGSERIAL PRIMARY KEY,
    planning_run_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    employee_id BIGINT,
    score INTEGER NOT NULL,
    assigned BOOLEAN NOT NULL,
    explanation TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_assignments_planning_run
        FOREIGN KEY (planning_run_id) REFERENCES planning_runs (id) ON DELETE CASCADE,
    CONSTRAINT fk_assignments_task
        FOREIGN KEY (task_id) REFERENCES tasks (id),
    CONSTRAINT fk_assignments_employee
        FOREIGN KEY (employee_id) REFERENCES employees (id)
);
