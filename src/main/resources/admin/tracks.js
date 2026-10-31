import { API_URL, ADMIN_API_URL, checkResponse } from "./api.js";

const tracksBody = document.getElementById("tracks-body");
const searchInput = document.getElementById("search-input");
const refreshButton = document.getElementById("refresh-btn");
const emptyState = document.getElementById("empty-state");
const notice = document.getElementById("notice");

const tracksCount = document.getElementById("tracks-count");

const playerPanel = document.getElementById("player-panel");
const audioPlayer = document.getElementById("audio-player");
const playingTitle = document.getElementById("playing-title");
const closePlayerButton = document.getElementById("close-player");

let tracks = [];
let playingTrackId = null;

// GET /api/v1/tracks
export async function loadTracks() {
    refreshButton.disabled = true;
    showNotice("");

    try {
        const response = await fetch(API_URL, {
            cache: "no-store"
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        const data = await response.json();

        if (!Array.isArray(data)) {
            throw new Error("Некорректный ответ сервера");
        }

        tracks = data;

        updateStatistics();
        renderTracks();

    } catch (error) {
        tracks = [];

        updateStatistics();
        renderTracks();

        showNotice(
            `Не удалось загрузить каталог: ${error.message}`
        );

    } finally {
        refreshButton.disabled = false;
    }
}


// Статистика каталога
function updateStatistics() {
    if (tracksCount) {
        tracksCount.textContent = tracks.length;
    }
}


// Форматирование длительности
function formatDuration(durationMs) {
    const seconds = Math.floor(durationMs / 1000);

    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;

    return `${minutes}:${String(remainingSeconds).padStart(2, "0")}`;
}


// Создание ячейки таблицы
function createCell(text, className = "") {
    const td = document.createElement("td");

    td.textContent = text;

    if (className) {
        td.className = className;
    }

    return td;
}


// Создание кнопки действия
function createActionButton(label, action, trackId) {
    const button = document.createElement("button");

    button.type = "button";
    button.textContent = label;
    button.className = "action-btn";

    if (action === "delete") {
        button.classList.add("action-delete");
    }

    button.dataset.action = action;
    button.dataset.trackId = trackId;

    return button;
}


// Создание строки с треком
function createTrackRow(track) {
    const row = document.createElement("tr");

    // Обложка
    const coverCell = document.createElement("td");
    const coverBox = document.createElement("div");

    coverBox.className = "cover";

    if (track.cover_url) {
        const coverImage = document.createElement("img");

        coverImage.src = track.cover_url;
        coverImage.alt = "";
        coverImage.loading = "lazy";

        coverImage.addEventListener("error", () => {
            coverBox.textContent = "♫";
        });

        coverBox.appendChild(coverImage);
    } else {
        coverBox.textContent = "♫";
    }
    coverCell.appendChild(coverBox);

    // Название
    const titleCell = createCell(
        track.title,
        "track-title"
    );

    // Исполнители
    const artistNames = track.artists
        .map(artist => artist.name)
        .join(", ");

    const artistCell = createCell(
        artistNames || "Не указан"
    );

    // Альбомы
    const albumNames = track.albums
        .map(album => album.title)
        .join(", ");

    const albumCell = createCell(
        albumNames || "Без альбома"
    );

    // Длительность
    const durationCell = createCell(
        formatDuration(track.duration_ms)
    );

    // Кнопки
    const actionsCell = document.createElement("td");
    const actionsContainer = document.createElement("div");

    actionsContainer.className = "actions";

    actionsContainer.appendChild(
        createActionButton("Слушать", "play", track.id)
    );

    actionsContainer.appendChild(
        createActionButton("Изменить", "edit", track.id)
    );

    actionsContainer.appendChild(
        createActionButton("Удалить", "delete", track.id)
    );

    actionsCell.appendChild(actionsContainer);

    row.append(
        coverCell,
        titleCell,
        artistCell,
        albumCell,
        durationCell,
        actionsCell
    );

    return row;
}


// Отображение списка с учётом поиска
function renderTracks() {
    const query = searchInput.value.trim().toLowerCase();

    const filteredTracks = tracks.filter(track => {
        const title = track.title.toLowerCase();

        const artists = track.artists
            .map(artist => artist.name)
            .join(" ")
            .toLowerCase();

        return title.includes(query) ||
            artists.includes(query);
    });

    tracksBody.replaceChildren();

    const fragment = document.createDocumentFragment();

    filteredTracks.forEach(track => {
        fragment.appendChild(createTrackRow(track));
    });

    tracksBody.appendChild(fragment);

    emptyState.hidden = filteredTracks.length !== 0;

    emptyState.textContent = tracks.length === 0
        ? "В каталоге пока нет треков"
        : "По вашему запросу ничего не найдено";
}


// GET /api/v1/tracks/{id}/stream
function playTrack(track) {
    playingTrackId = track.id;

    playingTitle.textContent = track.title;
    audioPlayer.src = track.stream_url;

    playerPanel.hidden = false;

    audioPlayer.play().catch(() => {
        showNotice("Не удалось начать воспроизведение");
    });
}


// Остановка плеера
function closePlayer() {
    audioPlayer.pause();
    audioPlayer.removeAttribute("src");
    audioPlayer.load();

    playingTrackId = null;
    playerPanel.hidden = true;
}


// DELETE /api/v1/admin/tracks/{id}
async function deleteTrack(track) {
    const confirmed = confirm(
        `Удалить трек "${track.title}"? Это действие нельзя отменить.`
    );

    if (!confirmed) {
        return;
    }

    try {
        const response = await fetch(
            `${ADMIN_API_URL}/${track.id}`,
            {
                method: "DELETE"
            }
        );

        await checkResponse(response);

        if (playingTrackId === track.id) {
            closePlayer();
        }

        tracks = tracks.filter(item => item.id !== track.id);

        updateStatistics();
        renderTracks();

        showNotice("Трек успешно удалён", true);

    } catch (error) {
        showNotice(
            `Не удалось удалить трек: ${error.message}`
        );
    }
}


// Отображение сообщений
export function showNotice(message, success = false) {
    notice.textContent = message;
    notice.hidden = !message;

    notice.className = success
        ? "notice success"
        : "notice";
}

export function initTracks(onEditTrack) {
    // Обработка кнопок в таблице
    tracksBody.addEventListener("click", event => {
        const button = event.target.closest(
            "button[data-action]"
        );

        if (!button) {
            return;
        }

        const id = Number(button.dataset.trackId);
        const track = tracks.find(item => item.id === id);

        if (!track) {
            return;
        }

        if (button.dataset.action === "play") {
            playTrack(track);
        }

        if (button.dataset.action === "edit") {
            onEditTrack(track);
        }

        if (button.dataset.action === "delete") {
            deleteTrack(track);
        }
    });


    // Поиск
    searchInput.addEventListener("input", renderTracks);


    // Обновление каталога
    refreshButton.addEventListener("click", loadTracks);


    // Закрытие плеера
    closePlayerButton.addEventListener("click", closePlayer);
}
