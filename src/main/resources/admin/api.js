// Общие адреса API и обработка HTTP-ошибок.
export const API_URL = "/api/v1/tracks";
export const ADMIN_API_URL = "/api/v1/admin/tracks";
export const REFERENCES_URL = "/api/v1/admin/references";

// Читаем сообщение об HTTP-ошибке
export async function checkResponse(response) {
    if (response.ok) {
        return;
    }

    const body = await response.json().catch(() => null);

    throw new Error(
        body?.message || `HTTP ${response.status}`
    );
}
