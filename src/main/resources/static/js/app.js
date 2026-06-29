import { API, apiFetch } from "./api.js";
import { clearSession, getRole, getToken, getUserLabel, saveSession } from "./session.js";
import {
    renderAssignmentsTable,
    renderEmployeesTable,
    renderMetricCards,
    renderMyTasks,
    renderPermissions,
    renderPlanningSummary,
    renderSkillsTable,
    renderSkillOptions,
    renderTaskStatus,
    renderTasksTable,
    renderUsersTable
} from "./renderers.js";

const pageTitles = {
    overviewView: "Resumen",
    usersView: "Accesos",
    teamView: "Empleados",
    skillsView: "Skills",
    tasksView: "Tareas",
    myTasksView: "Mis tareas",
    planningView: "Planificacion"
};

const elements = {
    loginView: document.getElementById("loginView"),
    appView: document.getElementById("appView"),
    loginForm: document.getElementById("loginForm"),
    loginMessage: document.getElementById("loginMessage"),
    globalMessage: document.getElementById("globalMessage"),
    pageTitle: document.getElementById("pageTitle"),
    userBadge: document.getElementById("userBadge"),
    logoutButton: document.getElementById("logoutButton"),
    runPlanningButton: document.getElementById("runPlanningButton"),
    createUserForm: document.getElementById("createUserForm"),
    createSkillForm: document.getElementById("createSkillForm"),
    createEmployeeForm: document.getElementById("createEmployeeForm"),
    employeeSubmitButton: document.getElementById("employeeSubmitButton"),
    cancelEmployeeEditButton: document.getElementById("cancelEmployeeEditButton"),
    createTaskForm: document.getElementById("createTaskForm"),
    taskSubmitButton: document.getElementById("taskSubmitButton"),
    cancelTaskEditButton: document.getElementById("cancelTaskEditButton"),
    navLinks: [...document.querySelectorAll("[data-view-target]")],
    views: [...document.querySelectorAll(".view")],
    metricCards: document.getElementById("metricCards"),
    permissionsGrid: document.getElementById("permissionsGrid"),
    usersTableBody: document.getElementById("usersTableBody"),
    usersEmpty: document.getElementById("usersEmpty"),
    skillsTableBody: document.getElementById("skillsTableBody"),
    skillsEmpty: document.getElementById("skillsEmpty"),
    employeeSkillOptions: document.getElementById("employeeSkillOptions"),
    employeesTableBody: document.getElementById("employeesTableBody"),
    employeesEmpty: document.getElementById("employeesEmpty"),
    taskSkillOptions: document.getElementById("taskSkillOptions"),
    taskStatusList: document.getElementById("taskStatusList"),
    taskStatusEmpty: document.getElementById("taskStatusEmpty"),
    tasksTableBody: document.getElementById("tasksTableBody"),
    tasksEmpty: document.getElementById("tasksEmpty"),
    planningSummary: document.getElementById("planningSummary"),
    assignmentsTableBody: document.getElementById("assignmentsTableBody"),
    assignmentsEmpty: document.getElementById("assignmentsEmpty"),
    myTasksList: document.getElementById("myTasksList"),
    myTasksEmpty: document.getElementById("myTasksEmpty")
};

document.addEventListener("DOMContentLoaded", init);

function init() {
    elements.loginForm.addEventListener("submit", login);
    elements.logoutButton.addEventListener("click", logout);
    elements.runPlanningButton.addEventListener("click", runPlanning);
    elements.createUserForm.addEventListener("submit", createUser);
    elements.createSkillForm.addEventListener("submit", createSkill);
    elements.createEmployeeForm.addEventListener("submit", updateEmployee);
    elements.cancelEmployeeEditButton.addEventListener("click", resetEmployeeForm);
    elements.createTaskForm.addEventListener("submit", createTask);
    elements.cancelTaskEditButton.addEventListener("click", resetTaskForm);
    elements.navLinks.forEach((link) => {
        link.addEventListener("click", () => setActiveView(link.dataset.viewTarget));
    });

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

        saveSession(response);
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
        applyRoleVisibility();

        if (getRole() === "EMPLOYEE") {
            const myTasks = await apiFetch(API.myTasks);
            renderMyTasks(elements.myTasksList, elements.myTasksEmpty, myTasks, updateMyTaskStatus);
            return;
        }

        const [summary, statusRows, assignments, runs] = await Promise.all([
            apiFetch(API.planningSummary),
            apiFetch(API.taskStatus),
            apiFetch(API.assignments),
            apiFetch(API.planningRuns)
        ]);

        const [employees, tasks, skills] = await Promise.all([
            apiFetch(API.employees),
            apiFetch(API.tasks),
            apiFetch(API.skills)
        ]);
        const users = getRole() === "ADMIN" ? await apiFetch(API.users) : [];

        renderMetricCards(elements.metricCards, summary);
        renderPlanningSummary(elements.planningSummary, summary, runs);
        renderPermissions(elements.permissionsGrid, getRole());
        if (getRole() === "ADMIN") {
            renderUsersTable(elements.usersTableBody, elements.usersEmpty, users);
        }
        renderSkillsTable(elements.skillsTableBody, elements.skillsEmpty, skills, deleteSkill);
        renderSkillOptions(elements.employeeSkillOptions, skills, "skillIds");
        renderSkillOptions(elements.taskSkillOptions, skills, "requiredSkillIds");
        renderEmployeesTable(elements.employeesTableBody, elements.employeesEmpty, employees, editEmployee);
        renderTaskStatus(elements.taskStatusList, elements.taskStatusEmpty, statusRows);
        renderTasksTable(elements.tasksTableBody, elements.tasksEmpty, tasks, editTask, deleteTask);
        renderAssignmentsTable(elements.assignmentsTableBody, elements.assignmentsEmpty, assignments);
    } catch (error) {
        if (!getToken()) {
            showLogin();
        }
        showError(error.message);
    }
}

async function createUser(event) {
    event.preventDefault();
    const formData = new FormData(elements.createUserForm);
    const role = formData.get("role");
    await submitAndReload(API.users, {
        username: formData.get("username"),
        password: formData.get("password"),
        role
    }, elements.createUserForm, role === "EMPLOYEE" ? "Acceso creado y perfil de empleado generado" : "Acceso creado");
}

async function createSkill(event) {
    event.preventDefault();
    const formData = new FormData(elements.createSkillForm);
    await submitAndReload(API.skills, {
        name: formData.get("name")
    }, elements.createSkillForm, "Skill creada");
}

async function updateEmployee(event) {
    event.preventDefault();
    const formData = new FormData(elements.createEmployeeForm);
    const employeeId = elements.createEmployeeForm.dataset.editingId;
    if (!employeeId) {
        showError("Selecciona un empleado de la tabla para editarlo");
        return;
    }

    const saved = await submitAndReload(`${API.employees}/${employeeId}`, {
        name: formData.get("name"),
        email: formData.get("email"),
        maxWeeklyHours: Number(formData.get("maxWeeklyHours")),
        currentWeeklyHours: Number(formData.get("currentWeeklyHours")),
        seniorityLevel: formData.get("seniorityLevel"),
        skillIds: formData.getAll("skillIds").map(Number)
    }, elements.createEmployeeForm, "Empleado actualizado", "PUT");
    if (saved) {
        resetEmployeeForm();
    }
}

async function createTask(event) {
    event.preventDefault();
    const formData = new FormData(elements.createTaskForm);
    const taskId = elements.createTaskForm.dataset.editingId;
    const endpoint = taskId ? `${API.tasks}/${taskId}` : API.tasks;
    const method = taskId ? "PUT" : "POST";

    const saved = await submitAndReload(endpoint, {
        title: formData.get("title"),
        description: formData.get("description"),
        priority: formData.get("priority"),
        estimatedHours: Number(formData.get("estimatedHours")),
        deadline: formData.get("deadline"),
        requiredSkillIds: formData.getAll("requiredSkillIds").map(Number)
    }, elements.createTaskForm, taskId ? "Tarea actualizada" : "Tarea creada", method);
    if (saved) {
        resetTaskForm();
    }
}

async function submitAndReload(endpoint, payload, form, successMessage, method = "POST") {
    const submitButton = form.querySelector("button[type='submit']");
    const originalText = submitButton?.textContent;
    setButtonLoading(submitButton, true, "Guardando...");

    try {
        await apiFetch(endpoint, {
            method,
            body: JSON.stringify(payload)
        });
        form.reset();
        showSuccess(successMessage);
        await loadDashboard();
        return true;
    } catch (error) {
        showError(error.message);
        return false;
    } finally {
        setButtonLoading(submitButton, false, originalText || "Guardar");
    }
}

function editEmployee(employee) {
    elements.createEmployeeForm.dataset.editingId = employee.id;
    elements.createEmployeeForm.classList.remove("hidden");
    elements.createEmployeeForm.elements.name.value = employee.name || "";
    elements.createEmployeeForm.elements.email.value = employee.email || "";
    elements.createEmployeeForm.elements.maxWeeklyHours.value = employee.maxWeeklyHours ?? 40;
    elements.createEmployeeForm.elements.currentWeeklyHours.value = employee.currentWeeklyHours ?? 0;
    elements.createEmployeeForm.elements.seniorityLevel.value = employee.seniorityLevel || "JUNIOR";
    setCheckedOptions(elements.employeeSkillOptions, employee.skills);
    elements.employeeSubmitButton.textContent = "Actualizar empleado";
    elements.cancelEmployeeEditButton.classList.remove("hidden");
    elements.createEmployeeForm.scrollIntoView({ behavior: "smooth", block: "center" });
}

function resetEmployeeForm() {
    delete elements.createEmployeeForm.dataset.editingId;
    elements.createEmployeeForm.reset();
    clearCheckedOptions(elements.employeeSkillOptions);
    elements.employeeSubmitButton.textContent = "Actualizar empleado";
    elements.cancelEmployeeEditButton.classList.add("hidden");
    elements.createEmployeeForm.classList.add("hidden");
}

function editTask(task) {
    elements.createTaskForm.dataset.editingId = task.id;
    elements.createTaskForm.elements.title.value = task.title || "";
    elements.createTaskForm.elements.description.value = task.description || "";
    elements.createTaskForm.elements.priority.value = task.priority || "MEDIUM";
    elements.createTaskForm.elements.estimatedHours.value = task.estimatedHours ?? 4;
    elements.createTaskForm.elements.deadline.value = task.deadline || "";
    setCheckedOptions(elements.taskSkillOptions, task.requiredSkills);
    elements.taskSubmitButton.textContent = "Actualizar tarea";
    elements.cancelTaskEditButton.classList.remove("hidden");
    elements.createTaskForm.scrollIntoView({ behavior: "smooth", block: "center" });
}

async function deleteTask(taskId) {
    if (!confirm("Seguro que quieres borrar esta tarea?")) {
        return;
    }

    await deleteAndReload(`${API.tasks}/${taskId}`, "Tarea borrada");
}

function resetTaskForm() {
    delete elements.createTaskForm.dataset.editingId;
    elements.createTaskForm.reset();
    elements.createTaskForm.elements.priority.value = "LOW";
    elements.createTaskForm.elements.estimatedHours.value = 4;
    clearCheckedOptions(elements.taskSkillOptions);
    elements.taskSubmitButton.textContent = "Crear tarea";
    elements.cancelTaskEditButton.classList.add("hidden");
}

async function deleteSkill(skillId) {
    if (!confirm("Seguro que quieres borrar esta skill?")) {
        return;
    }

    await deleteAndReload(`${API.skills}/${skillId}`, "Skill borrada");
}

async function deleteAndReload(endpoint, successMessage) {
    try {
        await apiFetch(endpoint, { method: "DELETE" });
        showSuccess(successMessage);
        await loadDashboard();
    } catch (error) {
        showError(error.message);
    }
}

function setCheckedOptions(container, selectedItems = []) {
    const selectedIds = new Set((selectedItems || []).map((item) => String(item.id)));
    container.querySelectorAll("input[type='checkbox']").forEach((input) => {
        input.checked = selectedIds.has(input.value);
    });
}

function clearCheckedOptions(container) {
    container.querySelectorAll("input[type='checkbox']").forEach((input) => {
        input.checked = false;
    });
}

async function updateMyTaskStatus(taskId, status) {
    try {
        await apiFetch(`${API.myTasks}/${taskId}/status`, {
            method: "PATCH",
            body: JSON.stringify({ status })
        });
        showSuccess(status === "DONE" ? "Tarea completada" : "Tarea marcada como en progreso");
        await loadDashboard();
    } catch (error) {
        showError(error.message);
    }
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

function setActiveView(viewId) {
    const target = document.getElementById(viewId);
    if (target.classList.contains("role-hidden")) {
        viewId = firstVisibleViewId();
    }

    elements.views.forEach((view) => {
        view.classList.toggle("active", view.id === viewId);
    });

    elements.navLinks.forEach((link) => {
        link.classList.toggle("active", link.dataset.viewTarget === viewId);
    });

    elements.pageTitle.textContent = pageTitles[viewId] || "SmartOps Planner";
}

function showLogin() {
    elements.appView.classList.add("hidden");
    elements.loginView.classList.remove("hidden");
}

function showApp() {
    elements.loginView.classList.add("hidden");
    elements.appView.classList.remove("hidden");
    elements.userBadge.textContent = getUserLabel();
    applyRoleVisibility();
    setActiveView(firstVisibleViewId());
}

function applyRoleVisibility() {
    const role = getRole();
    const allowedViews = {
        ADMIN: ["overviewView", "usersView", "teamView", "skillsView", "tasksView", "planningView"],
        MANAGER: ["overviewView", "teamView", "skillsView", "tasksView", "planningView"],
        EMPLOYEE: ["myTasksView"]
    };
    const allowed = allowedViews[role] || ["overviewView"];

    elements.views.forEach((view) => {
        view.classList.toggle("role-hidden", !allowed.includes(view.id));
    });
    elements.navLinks.forEach((link) => {
        link.classList.toggle("role-hidden", !allowed.includes(link.dataset.viewTarget));
    });
}

function firstVisibleViewId() {
    const visibleLink = elements.navLinks.find((link) => !link.classList.contains("role-hidden"));
    return visibleLink ? visibleLink.dataset.viewTarget : "overviewView";
}

function showError(message, target = elements.globalMessage) {
    target.textContent = message || "No se pudo completar la operacion";
    target.className = target === elements.globalMessage ? "message global-message error" : "message error";
    target.scrollIntoView({ behavior: "smooth", block: "nearest" });
}

function showSuccess(message, target = elements.globalMessage) {
    target.textContent = message || "Operacion completada";
    target.className = target === elements.globalMessage ? "message global-message success" : "message success";
    target.scrollIntoView({ behavior: "smooth", block: "nearest" });
}

function clearMessage(target) {
    target.textContent = "";
    target.className = target === elements.globalMessage ? "message global-message" : "message";
}

function setButtonLoading(button, loading, text) {
    if (!button) {
        return;
    }
    button.disabled = loading;
    button.textContent = text;
}
