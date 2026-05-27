const API = {
    login: "/api/auth/login",
    planningSummary: "/api/dashboard/planning-summary",
    workload: "/api/dashboard/workload",
    taskStatus: "/api/dashboard/task-status",
    runPlanning: "/api/planning/run",
    assignments: "/api/planning/assignments",
    planningRuns: "/api/planning/runs"
};

const STORAGE = {
    token: "smartops.token",
    username: "smartops.username",
    role: "smartops.role"
};

const elements = {
    loginView: document.getElementById("loginView"),
    appView: document.getElementById("appView"),
    loginForm: document.getElementById("loginForm"),
    loginMessage: document.getElementById("loginMessage"),
    globalMessage: document.getElementById("globalMessage"),
    userBadge: document.getElementById("userBadge"),
    logoutButton: document.getElementById("logoutButton"),
    refreshButton: document.getElementById("refreshButton"),
    runPlanningButton: document.getElementById("runPlanningButton"),
    metricCards: document.getElementById("metricCards"),
    workloadTableBody: document.getElementById("workloadTableBody"),
    workloadEmpty: document.getElementById("workloadEmpty"),
    taskStatusList: document.getElementById("taskStatusList"),
    taskStatusEmpty: document.getElementById("taskStatusEmpty"),
    planningSummary: document.getElementById("planningSummary"),
    assignmentsTableBody: document.getElementById("assignmentsTableBody"),
    assignmentsEmpty: document.getElementById("assignmentsEmpty")
};

document.addEventListener("DOMContentLoaded", init);

function init() {
    elements.loginForm.addEventListener("submit", login);
    elements.logoutButton.addEventListener("click", logout);
    elements.refreshButton.addEventListener("click", loadDashboard);
    elements.runPlanningButton.addEventListener("click", runPlanning);

    if (getToken()) {
        showApp();
        loadDashboard();
    } else {
        showLogin();
    }
}

async function login(event) {
    event.preventDefault();
    clearMessage(elements.loginMessage);

    const formData = new FormData(elements.loginForm);
    const payload = {
        username: formData.get("username"),
        password: formData.get("password")
    };

    try {
        const response = await apiFetch(API.login, {
            method: "POST",
            body: JSON.stringify(payload)
        }, false);

        localStorage.setItem(STORAGE.token, response.token);
        localStorage.setItem(STORAGE.username, response.username);
        localStorage.setItem(STORAGE.role, response.role);

        elements.loginForm.reset();
        showApp();
        showSuccess("Sesion iniciada correctamente");
        await loadDashboard();
    } catch (error) {
        showError(error.message || "No se pudo iniciar sesion", elements.loginMessage);
    }
}

function logout() {
    clearSession();
    showLogin();
    showSuccess("Sesion cerrada", elements.loginMessage);
}

async function loadDashboard() {
    clearMessage(elements.globalMessage);
    try {
        await Promise.all([
            loadPlanningSummary(),
            loadWorkload(),
            loadTaskStatus(),
            loadAssignments()
        ]);
    } catch (error) {
        showError(error.message);
    }
}

async function loadPlanningSummary() {
    const summary = await apiFetch(API.planningSummary);
    renderMetricCards(summary);
    await loadPlanningRuns(summary);
}

async function loadWorkload() {
    const workload = await apiFetch(API.workload);
    renderWorkloadTable(workload);
}

async function loadTaskStatus() {
    const status = await apiFetch(API.taskStatus);
    renderTaskStatus(status);
}

async function runPlanning() {
    setButtonLoading(elements.runPlanningButton, true, "Ejecutando...");

    try {
        const result = await apiFetch(API.runPlanning, { method: "POST" });
        showSuccess(result.summary || "Planificacion ejecutada correctamente");
        await loadDashboard();
    } catch (error) {
        showError(error.message);
    } finally {
        setButtonLoading(elements.runPlanningButton, false, "Ejecutar planificacion");
    }
}

async function loadAssignments() {
    const assignments = await apiFetch(API.assignments);
    renderAssignmentsTable(assignments);
}

async function loadPlanningRuns(summary) {
    const runs = await apiFetch(API.planningRuns);
    const sortedRuns = [...runs].sort((first, second) =>
        new Date(second.startedAt || 0) - new Date(first.startedAt || 0)
    );
    const latestRun = sortedRuns[0];
    const latestText = latestRun
        ? `${latestRun.status} - ${formatDate(latestRun.finishedAt || latestRun.startedAt)}`
        : "Sin ejecuciones";

    elements.planningSummary.innerHTML = `
        <div><strong>Ultima ejecucion:</strong> ${escapeHtml(latestText)}</div>
        <div><strong>Total runs:</strong> ${sortedRuns.length}</div>
        <div><strong>Resumen actual:</strong> ${summaryText(summary)}</div>
    `;
}

function renderMetricCards(summary) {
    const metrics = [
        ["Tareas asignadas", summary.assignedTasks ?? 0],
        ["Tareas pendientes", summary.pendingTasks ?? 0],
        ["Criticas pendientes", summary.criticalPendingTasks ?? 0],
        ["Score medio", formatNumber(summary.averageAssignmentScore ?? 0)],
        ["Ultima ejecucion", summary.latestPlanningRunStatus || "Sin datos"]
    ];

    elements.metricCards.innerHTML = metrics.map(([label, value]) => `
        <article class="metric-card">
            <span>${escapeHtml(label)}</span>
            <strong>${escapeHtml(String(value))}</strong>
        </article>
    `).join("");
}

function renderWorkloadTable(workload) {
    elements.workloadTableBody.innerHTML = "";
    const hasRows = Array.isArray(workload) && workload.length > 0;
    elements.workloadEmpty.classList.toggle("hidden", hasRows);

    if (!hasRows) {
        return;
    }

    elements.workloadTableBody.innerHTML = workload.map((employee) => `
        <tr>
            <td>${escapeHtml(employee.employeeName)}</td>
            <td>${employee.currentWeeklyHours ?? 0} / ${employee.maxWeeklyHours ?? 0}</td>
            <td>${employee.remainingWeeklyHours ?? 0} h libres</td>
            <td>${formatNumber(employee.workloadPercentage ?? 0)}%</td>
        </tr>
    `).join("");
}

function renderTaskStatus(statusRows) {
    elements.taskStatusList.innerHTML = "";
    const rows = Array.isArray(statusRows) ? statusRows.filter((row) => (row.count ?? 0) > 0) : [];
    elements.taskStatusEmpty.classList.toggle("hidden", rows.length > 0);

    if (rows.length === 0) {
        return;
    }

    elements.taskStatusList.innerHTML = rows.map((row) => {
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

function renderAssignmentsTable(assignments) {
    elements.assignmentsTableBody.innerHTML = "";
    const hasRows = Array.isArray(assignments) && assignments.length > 0;
    elements.assignmentsEmpty.classList.toggle("hidden", hasRows);

    if (!hasRows) {
        return;
    }

    elements.assignmentsTableBody.innerHTML = assignments.map((assignment) => {
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

async function apiFetch(endpoint, options = {}, includeAuth = true) {
    const headers = {
        "Accept": "application/json",
        ...(options.body ? { "Content-Type": "application/json" } : {}),
        ...(options.headers || {})
    };

    if (includeAuth && getToken()) {
        headers.Authorization = `Bearer ${getToken()}`;
    }

    let response;
    try {
        response = await fetch(endpoint, { ...options, headers });
    } catch (error) {
        throw new Error("No se pudo conectar con la API");
    }

    if (response.status === 401) {
        if (includeAuth) {
            clearSession();
            showLogin();
            throw new Error("Sesion caducada o no autenticada");
        }

        throw new Error("Usuario o password incorrectos");
    }

    if (response.status === 403) {
        throw new Error("No tienes permisos para acceder a esta funcionalidad");
    }

    if (!response.ok) {
        throw new Error("No se pudo completar la operacion");
    }

    if (response.status === 204) {
        return null;
    }

    return response.json();
}

function showLogin() {
    elements.appView.classList.add("hidden");
    elements.loginView.classList.remove("hidden");
}

function showApp() {
    const username = localStorage.getItem(STORAGE.username) || "Usuario";
    const role = localStorage.getItem(STORAGE.role) || "Rol";

    elements.loginView.classList.add("hidden");
    elements.appView.classList.remove("hidden");
    elements.userBadge.textContent = `${username} · ${role}`;
}

function showError(message, target = elements.globalMessage) {
    target.textContent = message || "No se pudo completar la operacion";
    target.className = target === elements.globalMessage ? "message global-message error" : "message error";
}

function showSuccess(message, target = elements.globalMessage) {
    target.textContent = message || "Operacion completada";
    target.className = target === elements.globalMessage ? "message global-message success" : "message success";
}

function clearMessage(target) {
    target.textContent = "";
    target.className = target === elements.globalMessage ? "message global-message" : "message";
}

function clearSession() {
    localStorage.removeItem(STORAGE.token);
    localStorage.removeItem(STORAGE.username);
    localStorage.removeItem(STORAGE.role);
}

function getToken() {
    return localStorage.getItem(STORAGE.token);
}

function setButtonLoading(button, loading, text) {
    button.disabled = loading;
    button.textContent = text;
}

function summaryText(summary) {
    if (!summary) {
        return "Sin datos";
    }

    return `${summary.assignedTasks ?? 0} asignadas, ${summary.pendingTasks ?? 0} pendientes`;
}

function formatNumber(value) {
    return Number(value).toLocaleString("es-ES", {
        maximumFractionDigits: 2
    });
}

function formatDate(value) {
    if (!value) {
        return "Sin fecha";
    }

    return new Date(value).toLocaleString("es-ES", {
        dateStyle: "short",
        timeStyle: "short"
    });
}

function clamp(value, min, max) {
    return Math.min(Math.max(Number(value) || 0, min), max);
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}
