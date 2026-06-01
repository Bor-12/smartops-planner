export function formatNumber(value) {
    return Number(value).toLocaleString("es-ES", {
        maximumFractionDigits: 2
    });
}

export function formatDate(value) {
    if (!value) {
        return "Sin fecha";
    }

    return new Date(value).toLocaleString("es-ES", {
        dateStyle: "short",
        timeStyle: "short"
    });
}

export function clamp(value, min, max) {
    return Math.min(Math.max(Number(value) || 0, min), max);
}

export function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}
