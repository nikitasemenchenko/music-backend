import { ADMIN_API_URL, checkResponse } from "./api.js";
import { loadTracks, showNotice } from "./tracks.js";
import { loadReferences, getSelectedIds, renderReferenceSelects, createReference } from "./references.js";

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

let editingTrackId = null;

// Открываем форму создания или редактирования
export async function openTrackForm(track = null) {
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
    originalDurationMs = track?.duration_ms ?? null;

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
        ? String(track.duration_ms / 1000)
        : "";

    releaseDateInput.value = track?.release_date || "";

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

export function initTrackForm() {
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
            artist_ids: artistIds,
            album_ids: albumIds,
            genre_ids: genreIds,
            duration_ms: durationMs,
            release_date: releaseDateInput.value || null
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

}
