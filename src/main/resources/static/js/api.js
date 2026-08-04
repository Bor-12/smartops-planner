import { clearSession, getToken } from "./session.js";

export const API = {
    login: "/api/auth/login",
    register: "/api/auth/register",
    users: "/api/users",
    employees: "/api/employees",
    tasks: "/api/tasks",
    skills: "/api/skills",
    myTasks: "/api/my-tasks",
    planningSummary: "/api/dashboard/planning-summary",
    workload: "/api/dashboard/workload",
    taskStatus: "/api/dashboard/task-status",
    runPlanning: "/api/planning/run",
    assignments: "/api/planning/assignments",
    planningRuns: "/api/planning/runs"
};

export async function apiFetch(endpoint, options = {}, includeAuth = true) {
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
            throw new Error("Sesion caducada o no autenticada");
        }

        throw new Error("Usuario o contraseña incorrectos");
    }

    if (response.status === 403) {
        throw new Error("No tienes permisos para acceder a esta funcionalidad");
    }

    if (!response.ok) {
        let message = "No se pudo completar la operacion";
        try {
            const error = await response.json();
            message = error.message || error.error || message;
        } catch {
            // Keep the generic message when the response has no JSON body.
        }
        throw new Error(message);
    }

    if (response.status === 204) {
        return null;
    }

    return response.json();
}
