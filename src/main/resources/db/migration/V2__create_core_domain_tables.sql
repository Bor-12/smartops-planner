CREATE TABLE skills (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE employees (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    max_weekly_hours INTEGER NOT NULL,
    current_weekly_hours INTEGER NOT NULL DEFAULT 0,
    seniority_level VARCHAR(50) NOT NULL
);

CREATE TABLE employee_skills (
    employee_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    PRIMARY KEY (employee_id, skill_id),
    CONSTRAINT fk_employee_skills_employee
        FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE CASCADE,
    CONSTRAINT fk_employee_skills_skill
        FOREIGN KEY (skill_id) REFERENCES skills (id) ON DELETE CASCADE
);

CREATE TABLE tasks (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    priority VARCHAR(50) NOT NULL,
    estimated_hours INTEGER NOT NULL,
    deadline DATE,
    status VARCHAR(50) NOT NULL,
    assigned_employee_id BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_tasks_assigned_employee
        FOREIGN KEY (assigned_employee_id) REFERENCES employees (id)
);

CREATE TABLE task_required_skills (
    task_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    PRIMARY KEY (task_id, skill_id),
    CONSTRAINT fk_task_required_skills_task
        FOREIGN KEY (task_id) REFERENCES tasks (id) ON DELETE CASCADE,
    CONSTRAINT fk_task_required_skills_skill
        FOREIGN KEY (skill_id) REFERENCES skills (id) ON DELETE CASCADE
);
