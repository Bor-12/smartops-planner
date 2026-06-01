const STORAGE = {
    token: "smartops.token",
    username: "smartops.username",
    role: "smartops.role"
};

export function saveSession(authResponse) {
    localStorage.setItem(STORAGE.token, authResponse.token);
    localStorage.setItem(STORAGE.username, authResponse.username);
    localStorage.setItem(STORAGE.role, authResponse.role);
}

export function clearSession() {
    localStorage.removeItem(STORAGE.token);
    localStorage.removeItem(STORAGE.username);
    localStorage.removeItem(STORAGE.role);
}

export function getToken() {
    return localStorage.getItem(STORAGE.token);
}

export function getUserLabel() {
    const username = localStorage.getItem(STORAGE.username) || "Usuario";
    const role = localStorage.getItem(STORAGE.role) || "Rol";
    return `${username} - ${role}`;
}

export function getRole() {
    return localStorage.getItem(STORAGE.role) || "";
}
