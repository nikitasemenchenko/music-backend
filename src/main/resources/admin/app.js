const API_URL = "/api/v1/tracks";
const ADMIN_API_URL = "/api/v1/admin/tracks";

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
async function loadTracks() {
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

    if (track.coverUrl) {
        const coverImage = document.createElement("img");

        coverImage.src = track.coverUrl;
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
        formatDuration(track.durationMs)
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
    audioPlayer.src = track.streamUrl;

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

        if (!response.ok) {
            const error = await response.json().catch(() => null);

            throw new Error(
                error?.message || `HTTP ${response.status}`
            );
        }

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
function showNotice(message, success = false) {
    notice.textContent = message;
    notice.hidden = !message;

    notice.className = success
        ? "notice success"
        : "notice";
}


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
        openTrackForm(track);
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


// Первая загрузка при открытии страницы
loadTracks();


const REFERENCES_URL = "/api/v1/admin/references";

const trackDialog = document.getElementById("track-dialog");
const trackForm = document.getElementById("track-form");
const formTitle = document.getElementById("form-title");
const formError = document.getElementById("form-error");
const saveTrackButton = document.getElementById("save-track-btn");

const titleInput = document.getElementById("track-title");
const durationInput = document.getElementById("track-duration");
const releaseDateInput = document.getElementById("track-release-date");

const artistsSelect = document.getElementById("track-artists");
const albumsSelect = document.getElementById("track-albums");
const genresSelect = document.getElementById("track-genres");

const audioInput = document.getElementById("track-audio");
const coverInput = document.getElementById("track-cover");

const referenceSearchInputs = {
    artists: document.getElementById("artists-search"),
    albums: document.getElementById("albums-search"),
    genres: document.getElementById("genres-search")
};

const referenceSelects = {
    artists: artistsSelect,
    albums: albumsSelect,
    genres: genresSelect
};

const referenceTextKeys = {
    artists: "name",
    albums: "title",
    genres: "name"
};

let selectedReferenceIds = {
    artists: new Set(),
    albums: new Set(),
    genres: new Set()
};

// Определяем продолжительность выбранного MP3
function readAudioDurationMs(file) {
    return new Promise(resolve => {
        const audio = document.createElement("audio");
        const objectUrl = URL.createObjectURL(file);

        let finished = false;

        const timeout = setTimeout(() => {
            finish(null);
        }, 10000);

        function finish(durationMs) {
            if (finished) return;

            finished = true;
            clearTimeout(timeout);

            audio.onloadedmetadata = null;
            audio.ondurationchange = null;
            audio.onerror = null;

            audio.removeAttribute("src");
            audio.load();

            URL.revokeObjectURL(objectUrl);

            resolve(durationMs);
        }

        function checkDuration() {
            const durationMs = Math.round(
                audio.duration * 1000
            );

            if (
                Number.isFinite(durationMs) &&
                durationMs > 0 &&
                durationMs <= 2147483647
            ) {
                finish(durationMs);
            }
        }

        audio.onloadedmetadata = checkDuration;
        audio.ondurationchange = checkDuration;
        audio.onerror = () => finish(null);

        audio.preload = "metadata";
        audio.src = objectUrl;
        audio.load();
    });
}

// Защита от устаревших асинхронных результатов
let durationRequestId = 0;
let durationLoading = false;
let originalDurationMs = null;

audioInput.addEventListener("change", async () => {
    const requestId = ++durationRequestId;
    const file = audioInput.files[0];

    showFormError("");

    // Если при редактировании убрали выбранный новый MP3,
    // восстанавливаем прежнюю длительность
    if (!file) {
        durationLoading = false;
        saveTrackButton.disabled = false;

        durationInput.value = originalDurationMs === null
            ? ""
            : String(originalDurationMs / 1000);

        return;
    }

    durationLoading = true;
    saveTrackButton.disabled = true;

    durationInput.value = "";

    try {
        const durationMs = await readAudioDurationMs(file);

        if (requestId !== durationRequestId) return;

        if (durationMs === null) {
            showFormError(
                "Не удалось определить длительность MP3. " +
                "Укажи её вручную."
            );
            return;
        }

        durationInput.value = String(durationMs / 1000);

    } finally {
        if (requestId === durationRequestId) {
            durationLoading = false;
            saveTrackButton.disabled = false;

            durationInput.placeholder = "Например, 180";
        }
    }
});


let editingTrackId = null;
let references = {
    artists: [],
    albums: [],
    genres: []
};


// Получаем референсы из Ktor
async function loadReferences() {
    const response = await fetch(REFERENCES_URL, {
        cache: "no-store"
    });

    await checkResponse(response);

    references = await response.json();
}


// Читаем выбранные ID из select
function getSelectedIds(select) {
    const type = select.dataset.referenceType;

    return Array.from(selectedReferenceIds[type]);
}


// Перерисовываем варианты
function fillSelect(type) {
    const select = referenceSelects[type];
    const items = references[type];
    const textKey = referenceTextKeys[type];

    const query = referenceSearchInputs[type]
        .value.trim().toLocaleLowerCase();

    const selectedIds = selectedReferenceIds[type];

    select.replaceChildren();

    items.forEach(item => {
        const label = item[textKey];

        // Показываем найденные и уже выбранные элементы
        if (
            !label.toLocaleLowerCase().includes(query) &&
            !selectedIds.has(item.id)
        ) {
            return;
        }

        const option = document.createElement("option");

        option.value = String(item.id);
        option.textContent = label;
        option.selected = selectedIds.has(item.id);

        select.appendChild(option);
    });
}


// Устанавливаем выбранные ID и обновляем списки
function renderReferenceSelects(selected = {}) {
    selectedReferenceIds = {
        artists: new Set(selected.artistIds || []),
        albums: new Set(selected.albumIds || []),
        genres: new Set(selected.genreIds || [])
    };

    Object.keys(referenceSelects).forEach(fillSelect);
}

Object.entries(referenceSelects).forEach(([type, select]) => {

    select.addEventListener("change", () => {
        const selectedIds = new Set(
            Array.from(select.selectedOptions)
                .map(option => Number(option.value))
        );

        // Обновляем выбор только среди отображаемых вариантов
        Array.from(select.options).forEach(option => {
            const id = Number(option.value);

            if (selectedIds.has(id)) {
                selectedReferenceIds[type].add(id);
            } else {
                selectedReferenceIds[type].delete(id);
            }
        });
    });

    referenceSearchInputs[type].addEventListener("input", () => {
        fillSelect(type);
    });
});

// Открываем форму создания или редактирования
async function openTrackForm(track = null) {
    try {
        await loadReferences();
    } catch (error) {
        showNotice(
            `Не удалось загрузить справочники: ${error.message}`
        );
        return;
    }

    editingTrackId = track ? track.id : null;

// Отменяем влияние предыдущего чтения MP3
    durationRequestId++;
    durationLoading = false;
    originalDurationMs = track?.durationMs ?? null;

    trackForm.reset();
    showFormError("");

    saveTrackButton.disabled = false;

    formTitle.textContent = track
        ? "Редактирование трека"
        : "Добавление трека";

    saveTrackButton.textContent = track
        ? "Сохранить изменения"
        : "Добавить трек";

    titleInput.value = track?.title || "";

    durationInput.value = track
        ? String(track.durationMs / 1000)
        : "";

    releaseDateInput.value = track?.releaseDate || "";

    audioInput.required = !track;

    renderReferenceSelects({
        artistIds: track?.artists.map(item => item.id) || [],
        albumIds: track?.albums.map(item => item.id) || [],
        genreIds: track?.genres.map(item => item.id) || []
    });

    trackDialog.showModal();
}


// Показываем ошибку внутри формы
function showFormError(message) {
    formError.textContent = message;
    formError.hidden = !message;
}


// Читаем сообщение об HTTP-ошибке
async function checkResponse(response) {
    if (response.ok) {
        return;
    }

    const body = await response.json().catch(() => null);

    throw new Error(
        body?.message || `HTTP ${response.status}`
    );
}


// Создание или обновление трека
trackForm.addEventListener("submit", async event => {
    event.preventDefault();
    showFormError("");

    if (durationLoading) {
        showFormError(
            "Дождись определения длительности MP3"
        );
        return;
    }

    const artistIds = getSelectedIds(artistsSelect);
    const albumIds = getSelectedIds(albumsSelect);
    const genreIds = getSelectedIds(genresSelect);

    if (artistIds.length === 0) {
        showFormError("Выбери хотя бы одного исполнителя");
        return;
    }

    const durationMs = Math.round(
        Number(durationInput.value) * 1000
    );

    if (
        !Number.isInteger(durationMs) ||
        durationMs <= 0 ||
        durationMs > 2147483647
    ) {
        showFormError("Некорректная длительность");
        return;
    }

    const audioFile = audioInput.files[0];
    const coverFile = coverInput.files[0];

    if (editingTrackId === null && !audioFile) {
        showFormError("Для нового трека требуется MP3");
        return;
    }

    const metadata = {
        title: titleInput.value.trim(),
        artistIds,
        albumIds,
        genreIds,
        durationMs,
        releaseDate: releaseDateInput.value || null
    };

    // Формируем multipart/form-data
    const formData = new FormData();

    formData.append(
        "metadata",
        JSON.stringify(metadata)
    );

    if (audioFile) {
        formData.append("audio", audioFile);
    }

    if (coverFile) {
        formData.append("cover", coverFile);
    }

    const isEditing = editingTrackId !== null;

    const url = isEditing
        ? `${ADMIN_API_URL}/${editingTrackId}`
        : ADMIN_API_URL;

    saveTrackButton.disabled = true;

    try {
        const response = await fetch(url, {
            method: isEditing ? "PUT" : "POST",
            body: formData
        });

        await checkResponse(response);

        trackDialog.close();

        // Обновляем каталог после сохранения
        await loadTracks();

        showNotice(
            isEditing
                ? "Трек успешно обновлён"
                : "Трек успешно добавлен",
            true
        );

    } catch (error) {
        showFormError(
            `Не удалось сохранить трек: ${error.message}`
        );

    } finally {
        saveTrackButton.disabled = false;
    }
});


// Создаём справочник из формы
async function createReference(type) {
    const names = {
        artists: "исполнителя",
        albums: "альбома",
        genres: "жанра"
    };

    const name = prompt(`Название ${names[type]}:`);

    if (name === null) {
        return;
    }

    const value = name.trim();

    if (!value) {
        showFormError("Название не должно быть пустым");
        return;
    }

    const previousSelection = {
        artistIds: getSelectedIds(artistsSelect),
        albumIds: getSelectedIds(albumsSelect),
        genreIds: getSelectedIds(genresSelect)
    };

    const requestBody = type === "albums"
        ? { title: value, releaseDate: null }
        : { name: value };

    const response = await fetch(
        `${REFERENCES_URL}/${type}`,
        {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(requestBody)
        }
    );

    await checkResponse(response);

    const created = await response.json();

    // Получаем обновлённый список
    await loadReferences();

    // Сохраняем прежний выбор и выбираем новую запись
    if (type === "artists") {
        previousSelection.artistIds.push(created.id);
    } else if (type === "albums") {
        previousSelection.albumIds.push(created.id);
    } else {
        previousSelection.genreIds.push(created.id);
    }

    renderReferenceSelects(previousSelection);
}


// Обработчики кнопок создания референсов
trackDialog.addEventListener("click", async event => {
    const button = event.target.closest(
        "button[data-create-reference]"
    );

    if (!button) {
        return;
    }

    button.disabled = true;
    showFormError("");

    try {
        await createReference(button.dataset.createReference);
    } catch (error) {
        showFormError(
            `Не удалось создать запись: ${error.message}`
        );
    } finally {
        button.disabled = false;
    }
});


// Кнопка добавления трека
document.getElementById("add-track-btn")
    .addEventListener("click", () => openTrackForm());


// Кнопки закрытия формы
document.getElementById("cancel-track-btn")
    .addEventListener("click", () => trackDialog.close());

document.getElementById("cancel-track-footer")
    .addEventListener("click", () => trackDialog.close());
