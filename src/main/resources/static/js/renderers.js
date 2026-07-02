import { clamp, escapeHtml, formatDate, formatNumber } from "./format.js";

function formatPlanningStatus(status) {
    const labels = {
        RUNNING: "En ejecucion",
        COMPLETED: "Completada",
        FAILED: "Fallida"
    };

    return labels[status] || "Sin datos";
}

export function renderMetricCards(container, summary) {
    const metrics = [
        { label: "Tareas asignadas", value: summary.assignedTasks ?? 0 },
        { label: "Tareas pendientes", value: summary.pendingTasks ?? 0 },
        { label: "Criticas pendientes", value: summary.criticalPendingTasks ?? 0 },
        { label: "Score medio", value: formatNumber(summary.averageAssignmentScore ?? 0) }
    ];

    container.innerHTML = metrics.map((metric) => `
        <article class="metric-card">
            <span>${escapeHtml(metric.label)}</span>
            <strong>${escapeHtml(String(metric.value))}</strong>
        </article>
    `).join("");
}

export function renderPlanningSummary(container, summary, runs) {
    const sortedRuns = [...runs].sort((first, second) =>
        new Date(second.startedAt || 0) - new Date(first.startedAt || 0)
    );
    const latestRun = sortedRuns[0];
    const latestText = latestRun
        ? `${formatPlanningStatus(latestRun.status)} - ${formatDate(latestRun.finishedAt || latestRun.startedAt)}`
        : "Sin ejecuciones";

    container.innerHTML = `
        <article class="summary-item">
            <span>Ultima ejecucion</span>
            <strong>${escapeHtml(latestText)}</strong>
        </article>
        <article class="summary-item">
            <span>Ejecuciones</span>
            <strong>${sortedRuns.length}</strong>
        </article>
        <article class="summary-item">
            <span>Resumen actual</span>
            <strong>${summary.assignedTasks ?? 0} asignadas, ${summary.pendingTasks ?? 0} pendientes</strong>
        </article>
    `;
}

export function renderWorkloadTable(tableBody, emptyElement, workload) {
    tableBody.innerHTML = "";
    const hasRows = Array.isArray(workload) && workload.length > 0;
    emptyElement.classList.toggle("hidden", hasRows);

    if (!hasRows) {
        return;
    }

    tableBody.innerHTML = workload.map((employee) => `
        <tr>
            <td>${escapeHtml(employee.employeeName)}</td>
            <td>${employee.currentWeeklyHours ?? 0} / ${employee.maxWeeklyHours ?? 0}</td>
            <td>${employee.remainingWeeklyHours ?? 0} h libres</td>
            <td>${formatNumber(employee.workloadPercentage ?? 0)}%</td>
        </tr>
    `).join("");
}

export function renderUsersTable(tableBody, emptyElement, users) {
    tableBody.innerHTML = "";
    const hasRows = Array.isArray(users) && users.length > 0;
    emptyElement.classList.toggle("hidden", hasRows);

    if (!hasRows) {
        return;
    }

    tableBody.innerHTML = users.map((user) => `
        <tr>
            <td><strong>${escapeHtml(user.username)}</strong></td>
            <td><span class="role-pill">${escapeHtml(user.role)}</span></td>
            <td>${escapeHtml(permissionText(user.role))}</td>
        </tr>
    `).join("");
}

export function renderPermissions(container, role) {
    const permissions = [
        ["ADMIN", "Gestion completa del panel"],
        ["MANAGER", "Gestion operativa y planificacion"],
        ["EMPLOYEE", "Consulta sus tareas asignadas y actualiza su estado"]
    ];

    container.innerHTML = permissions.map(([name, description]) => `
        <article class="permission-card ${name === role ? "current" : ""}">
            <span>${escapeHtml(name)}</span>
            <strong>${escapeHtml(description)}</strong>
        </article>
    `).join("");
}

export function renderEmployeesTable(tableBody, emptyElement, employees, onEdit) {
    tableBody.innerHTML = "";
    const hasRows = Array.isArray(employees) && employees.length > 0;
    emptyElement.classList.toggle("hidden", hasRows);

    if (!hasRows) {
        return;
    }

    tableBody.innerHTML = employees.map((employee) => {
        const maxHours = employee.maxWeeklyHours ?? 0;
        const currentHours = employee.currentWeeklyHours ?? 0;
        const percentage = maxHours > 0 ? (currentHours / maxHours) * 100 : 0;
        return `
            <tr>
                <td>
                    <strong>${escapeHtml(employee.name)}</strong>
                    <span class="subtext">${escapeHtml(employee.email)}</span>
                </td>
                <td>${escapeHtml(employee.seniorityLevel)}</td>
                <td>${renderSkillTags(employee.skills)}</td>
                <td>${currentHours} / ${maxHours}</td>
                <td>
                    <div class="mini-progress" aria-label="Uso ${formatNumber(percentage)}%">
                        <span style="width: ${clamp(percentage, 0, 100)}%"></span>
                    </div>
                    <span class="subtext">${formatNumber(percentage)}%</span>
                </td>
                <td>
                    <div class="row-actions">
                        <button type="button" class="button tiny secondary" data-employee-edit="${employee.id}">Editar</button>
                    </div>
                </td>
            </tr>
        `;
    }).join("");

    tableBody.querySelectorAll("[data-employee-edit]").forEach((button) => {
        button.addEventListener("click", () => {
            const employee = employees.find((item) => String(item.id) === button.dataset.employeeEdit);
            if (employee) {
                onEdit(employee);
            }
        });
    });

}

export function renderSkillsTable(tableBody, emptyElement, skills, onDelete) {
    tableBody.innerHTML = "";
    const hasRows = Array.isArray(skills) && skills.length > 0;
    emptyElement.classList.toggle("hidden", hasRows);

    if (!hasRows) {
        return;
    }

    tableBody.innerHTML = skills.map((skill) => `
        <tr>
            <td><span class="tag">${escapeHtml(skill.name)}</span></td>
            <td>
                <button type="button" class="button tiny danger" data-skill-delete="${skill.id}">Borrar</button>
            </td>
        </tr>
    `).join("");

    tableBody.querySelectorAll("[data-skill-delete]").forEach((button) => {
        button.addEventListener("click", () => onDelete(button.dataset.skillDelete));
    });
}

export function renderTasksTable(tableBody, emptyElement, tasks, onEdit, onDelete) {
    tableBody.innerHTML = "";
    const hasRows = Array.isArray(tasks) && tasks.length > 0;
    emptyElement.classList.toggle("hidden", hasRows);

    if (!hasRows) {
        return;
    }

    tableBody.innerHTML = tasks.map((task) => `
        <tr>
            <td>
                <strong>${escapeHtml(task.title)}</strong>
                <span class="subtext">${escapeHtml(task.description || "Sin descripcion")}</span>
            </td>
            <td><span class="priority ${priorityClass(task.priority)}">${escapeHtml(task.priority)}</span></td>
            <td>${escapeHtml(task.status)}</td>
            <td>${task.estimatedHours ?? 0} h</td>
            <td>${escapeHtml(task.deadline || "Sin fecha")}</td>
            <td>${renderSkillTags(task.requiredSkills)}</td>
            <td>
                <div class="row-actions">
                    <button type="button" class="button tiny secondary" data-task-edit="${task.id}">Editar</button>
                    <button type="button" class="button tiny danger" data-task-delete="${task.id}">Borrar</button>
                </div>
            </td>
        </tr>
    `).join("");

    tableBody.querySelectorAll("[data-task-edit]").forEach((button) => {
        button.addEventListener("click", () => {
            const task = tasks.find((item) => String(item.id) === button.dataset.taskEdit);
            if (task) {
                onEdit(task);
            }
        });
    });

    tableBody.querySelectorAll("[data-task-delete]").forEach((button) => {
        button.addEventListener("click", () => onDelete(button.dataset.taskDelete));
    });
}

export function renderMyTasks(container, emptyElement, tasks, onStatusChange) {
    container.innerHTML = "";
    const visibleTasks = Array.isArray(tasks)
        ? tasks.filter((task) => task.status !== "DONE" && task.status !== "CANCELLED")
        : [];
    const hasRows = visibleTasks.length > 0;
    emptyElement.classList.toggle("hidden", hasRows);

    if (!hasRows) {
        return;
    }

    container.innerHTML = visibleTasks.map((task) => `
        <article class="task-card">
            <h3>${escapeHtml(task.title)}</h3>
            <p>${escapeHtml(task.description || "Sin descripcion")}</p>
            <div class="tag-list">
                <span class="priority ${priorityClass(task.priority)}">${escapeHtml(task.priority)}</span>
                <span class="tag">${escapeHtml(task.status)}</span>
                <span class="tag">${task.estimatedHours ?? 0} h</span>
                <span class="tag">${escapeHtml(task.deadline || "Sin fecha")}</span>
            </div>
            ${renderSkillTags(task.requiredSkills)}
            <div class="task-actions">
                <button type="button" class="button secondary" data-task-status="${task.id}:IN_PROGRESS" ${task.status === "IN_PROGRESS" ? "disabled" : ""}>Empezar</button>
                <button type="button" class="button primary" data-task-status="${task.id}:DONE">Completar</button>
            </div>
        </article>
    `).join("");

    container.querySelectorAll("[data-task-status]").forEach((button) => {
        button.addEventListener("click", () => {
            const [taskId, status] = button.dataset.taskStatus.split(":");
            onStatusChange(taskId, status);
        });
    });
}

export function renderSkillOptions(container, skills, fieldName) {
    if (!Array.isArray(skills) || skills.length === 0) {
        container.innerHTML = "<span class=\"subtext\">No hay skills disponibles</span>";
        return;
    }

    container.innerHTML = skills.map((skill) => `
        <label class="check-chip">
            <input type="checkbox" name="${fieldName}" value="${skill.id}">
            ${escapeHtml(skill.name)}
        </label>
    `).join("");
}

export function renderAlgorithm(container) {
    const rules = [
        ["Skills requeridas", "+20 por skill cumplida. Si falta una skill requerida, penaliza -25 y no puede asignarse."],
        ["Skills extra", "+5 por skill adicional, con maximo +10."],
        ["Seniority", "+15 si el nivel encaja con la prioridad. Penaliza -15 si queda corto."],
        ["Carga semanal", "+15 si tiene menos del 60%, +8 si esta entre 60% y 80%, -10 si supera 80%."],
        ["Capacidad", "+20 si caben las horas. Si supera su maximo semanal, penaliza -40 y queda no apto."],
        ["Deadline cercano", "-15 si vence en 3 dias o menos y la persona ya tiene carga relevante."]
    ];

    container.innerHTML = rules.map(([title, description]) => `
        <article class="algorithm-card">
            <h3>${escapeHtml(title)}</h3>
            <p>${escapeHtml(description)}</p>
        </article>
    `).join("");
}

export function renderTaskStatus(container, emptyElement, statusRows) {
    container.innerHTML = "";
    const rows = Array.isArray(statusRows) ? statusRows.filter((row) => (row.count ?? 0) > 0) : [];
    emptyElement.classList.toggle("hidden", rows.length > 0);

    if (rows.length === 0) {
        return;
    }

    container.innerHTML = rows.map((row) => {
        const percentage = row.percentage ?? 0;
        return `
            <div class="status-row">
                <strong>${escapeHtml(row.status)}</strong>
                <div class="status-track" aria-label="${escapeHtml(row.status)} ${formatNumber(percentage)}%">
                    <div class="status-bar" style="width: ${clamp(percentage, 0, 100)}%"></div>
                </div>
                <span>${row.count ?? 0} (${formatNumber(percentage)}%)</span>
            </div>
        `;
    }).join("");
}

export function renderAssignmentsTable(tableBody, emptyElement, assignments) {
    tableBody.innerHTML = "";
    const hasRows = Array.isArray(assignments) && assignments.length > 0;
    emptyElement.classList.toggle("hidden", hasRows);

    if (!hasRows) {
        return;
    }

    tableBody.innerHTML = assignments.map((assignment) => {
        const assigned = Boolean(assignment.assigned);
        return `
            <tr>
                <td>${escapeHtml(assignment.taskTitle || `Task #${assignment.taskId}`)}</td>
                <td>${escapeHtml(assignment.employeeName || "Sin asignar")}</td>
                <td>${assignment.score ?? 0}</td>
                <td><span class="badge ${assigned ? "success" : "warning"}">${assigned ? "Si" : "No"}</span></td>
                <td>${escapeHtml(assignment.explanation || "Sin explicacion")}</td>
            </tr>
        `;
    }).join("");
}

function renderSkillTags(skills) {
    if (!Array.isArray(skills) || skills.length === 0) {
        return "<span class=\"subtext\">Sin skills</span>";
    }

    return `
        <div class="tag-list">
            ${skills.map((skill) => `<span class="tag">${escapeHtml(skill.name)}</span>`).join("")}
        </div>
    `;
}

function permissionText(role) {
    const permissions = {
        ADMIN: "Todo el panel y gestion completa",
        MANAGER: "Gestion operativa, tareas, equipo y planificacion",
        EMPLOYEE: "Consulta de tareas propias"
    };

    return permissions[role] || "Permisos no definidos";
}

function priorityClass(priority) {
    const value = String(priority || "").toLowerCase();
    return ["low", "medium", "high", "urgent"].includes(value) ? value : "medium";
}
