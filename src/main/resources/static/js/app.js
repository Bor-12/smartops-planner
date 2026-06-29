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
    usersView: "Usuarios",
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
    createTaskForm: document.getElementById("createTaskForm"),
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
    elements.createEmployeeForm.addEventListener("submit", createEmployee);
    elements.createTaskForm.addEventListener("submit", createTask);
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
        renderSkillsTable(elements.skillsTableBody, elements.skillsEmpty, skills);
        renderSkillOptions(elements.employeeSkillOptions, skills, "skillIds");
        renderSkillOptions(elements.taskSkillOptions, skills, "requiredSkillIds");
        renderEmployeesTable(elements.employeesTableBody, elements.employeesEmpty, employees);
        renderTaskStatus(elements.taskStatusList, elements.taskStatusEmpty, statusRows);
        renderTasksTable(elements.tasksTableBody, elements.tasksEmpty, tasks);
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
    await submitAndReload(API.users, {
        username: formData.get("username"),
        password: formData.get("password"),
        role: formData.get("role")
    }, elements.createUserForm, "Usuario creado");
}

async function createSkill(event) {
    event.preventDefault();
    const formData = new FormData(elements.createSkillForm);
    await submitAndReload(API.skills, {
        name: formData.get("name")
    }, elements.createSkillForm, "Skill creada");
}

async function createEmployee(event) {
    event.preventDefault();
    const formData = new FormData(elements.createEmployeeForm);
    await submitAndReload(API.employees, {
        name: formData.get("name"),
        email: formData.get("email"),
        maxWeeklyHours: Number(formData.get("maxWeeklyHours")),
        currentWeeklyHours: Number(formData.get("currentWeeklyHours")),
        seniorityLevel: formData.get("seniorityLevel"),
        skillIds: formData.getAll("skillIds").map(Number)
    }, elements.createEmployeeForm, "Empleado creado");
}

async function createTask(event) {
    event.preventDefault();
    const formData = new FormData(elements.createTaskForm);
    await submitAndReload(API.tasks, {
        title: formData.get("title"),
        description: formData.get("description"),
        priority: formData.get("priority"),
        estimatedHours: Number(formData.get("estimatedHours")),
        deadline: formData.get("deadline"),
        requiredSkillIds: formData.getAll("requiredSkillIds").map(Number)
    }, elements.createTaskForm, "Tarea creada");
}

async function submitAndReload(endpoint, payload, form, successMessage) {
    const submitButton = form.querySelector("button[type='submit']");
    const originalText = submitButton?.textContent;
    setButtonLoading(submitButton, true, "Guardando...");

    try {
        await apiFetch(endpoint, {
            method: "POST",
            body: JSON.stringify(payload)
        });
        form.reset();
        showSuccess(successMessage);
        await loadDashboard();
    } catch (error) {
        showError(error.message);
    } finally {
        setButtonLoading(submitButton, false, originalText || "Guardar");
    }
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
