import { REFERENCES_URL, checkResponse } from "./api.js";
import { showNotice } from "./tracks.js";
import { requestAlbumDetails } from "./album-dialog.js";

const referenceSearchInputs = {
    artists: document.getElementById("artists-search"),
    albums: document.getElementById("albums-search"),
    genres: document.getElementById("genres-search")
};

const referenceSelects = {
    artists: document.getElementById("track-artists"),
    albums: document.getElementById("track-albums"),
    genres: document.getElementById("track-genres")
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

let references = {
    artists: [],
    albums: [],
    genres: []
};

// Получаем референсы из Ktor
export async function loadReferences() {
    const response = await fetch(REFERENCES_URL, {
        cache: "no-store"
    });

    await checkResponse(response);

    references = await response.json();
}


// Читаем выбранные ID из select
export function getSelectedIds(select) {
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
export function renderReferenceSelects(selected = {}) {
    selectedReferenceIds = {
        artists: new Set(selected.artistIds || []),
        albums: new Set(selected.albumIds || []),
        genres: new Set(selected.genreIds || [])
    };

    Object.keys(referenceSelects).forEach(fillSelect);
}

const referencesDialog = document.getElementById("references-dialog");
const manageReferencesButton = document.getElementById("manage-references-btn");
const closeReferencesButton = document.getElementById("close-references-btn");
const referencesNotice = document.getElementById("references-notice");

const referenceManagementLists = {
    artists: document.getElementById("reference-artists-list"),
    albums: document.getElementById("reference-albums-list"),
    genres: document.getElementById("reference-genres-list")
};

const referenceNames = {
    artists: "исполнителя",
    albums: "альбома",
    genres: "жанра"
};

const managementSearchInputs = {
    artists: document.getElementById("manage-artists-search"),
    albums: document.getElementById("manage-albums-search"),
    genres: document.getElementById("manage-genres-search")
};

function renderReferenceManagement() {
    Object.entries(referenceManagementLists).forEach(([type, container]) => {
        container.replaceChildren();

        const query = managementSearchInputs[type]
            .value.trim().toLocaleLowerCase();

        const items = references[type] || [];

        const filteredItems = items.filter(item => {
            const name = item[referenceTextKeys[type]];

            return name.toLocaleLowerCase().includes(query);
        });

        if (filteredItems.length === 0) {
            const empty = document.createElement("p");

            empty.textContent = items.length === 0
                ? "Список пуст"
                : "Ничего не найдено";

            empty.className = "empty-state";
            container.appendChild(empty);
            return;
        }

        filteredItems.forEach(item => {
            const row = document.createElement("div");
            row.className = "reference-management-item";

            const name = document.createElement("span");
            name.textContent = item[referenceTextKeys[type]];

            const deleteButton = document.createElement("button");
            deleteButton.type = "button";
            deleteButton.textContent = "Удалить";
            deleteButton.className = "action-btn action-delete";

            deleteButton.dataset.deleteReference = "";
            deleteButton.dataset.referenceType = type;
            deleteButton.dataset.referenceId = item.id;

            row.append(name, deleteButton);
            container.appendChild(row);
        });
    });
}

function showReferencesNotice(message, success = false) {
    referencesNotice.textContent = message;
    referencesNotice.hidden = !message;

    referencesNotice.className = success
        ? "references-notice success"
        : "references-notice";
}

// Единое создание справочника: из формы трека или окна управления
export async function createReference(type, source = "track") {
    const prepare = await prepareReferenceRequest(type);

    if (prepare === null) {
        return null;
    }

    const response = await fetch(`${REFERENCES_URL}/${type}`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(prepare.body)
    });

    await checkResponse(response);

    const created = await response.json();
    await loadReferences();

    if (source === "management") {
        managementSearchInputs[type].value = "";
        renderReferenceManagement();
    } else {
        selectedReferenceIds[type].add(created.id);
        fillSelect(type);
    }

    return prepare.value;
}

async function prepareReferenceRequest(type) {
    if (type === "albums") {
        const album = await requestAlbumDetails();

        if (album === null) {
            return null;
        }

        return {
            value: album.title,
            body: {
                title: album.title,
                release_date: album.releaseDate
            }
        };
    }

    const name = prompt(`Название ${referenceNames[type]}:`);

    if (name === null) {
        return null;
    }

    const value = name.trim();

    if (!value) {
        throw new Error("Название не должно быть пустым");
    }

    return {
        value,
        body: {
            name: value
        }
    };
}

export function initReferences() {
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

    Object.values(managementSearchInputs).forEach(input => {
        input.addEventListener("input", renderReferenceManagement);
    });

    manageReferencesButton.addEventListener("click", async () => {
        manageReferencesButton.disabled = true;

        try {
            await loadReferences();

            renderReferenceManagement();
            showReferencesNotice("");

            referencesDialog.showModal();

        } catch (error) {
            showNotice(
                `Не удалось загрузить референсы: ${error.message}`
            );

        } finally {
            manageReferencesButton.disabled = false;
        }
    });

    closeReferencesButton.addEventListener("click", () => {
        referencesDialog.close();
    });

    referencesDialog.addEventListener("click", async event => {
        const createButton = event.target.closest(
            "button[data-manage-create]"
        );

        if (createButton) {
            createButton.disabled = true;
            showReferencesNotice("");

            try {
                const name = await createReference(
                    createButton.dataset.manageCreate,
                    "management"
                );

                if (name !== null) {
                    showReferencesNotice(`"${name}" успешно добавлен.`, true);
                }
            } catch (error) {
                showReferencesNotice(`Не удалось создать запись: ${error.message}`);
            } finally {
                createButton.disabled = false;
            }
            return;
        }

        const button = event.target.closest(
            "button[data-delete-reference]"
        );

        if (!button) {
            return;
        }

        const type = button.dataset.referenceType;
        const id = Number(button.dataset.referenceId);

        const item = references[type]?.find(
            reference => reference.id === id
        );

        if (!item) {
            return;
        }

        const name = item[referenceTextKeys[type]];

        const confirmed = confirm(
            `Удалить "${name}"? Это действие нельзя отменить.`
        );

        if (!confirmed) {
            return;
        }

        button.disabled = true;
        showReferencesNotice("");

        try {
            const response = await fetch(
                `${REFERENCES_URL}/${type}/${id}`,
                {
                    method: "DELETE"
                }
            );

            if (response.status === 409) {
                throw new Error("Запись используется треками. Сначала измените связи.");
            }

            await checkResponse(response);

            await loadReferences();

            renderReferenceManagement();

            showReferencesNotice(
                `"${name}" успешно удалён.`,
                true
            );

        } catch (error) {
            showReferencesNotice(
                `Не удалось удалить ${referenceNames[type]}: ${error.message}`
            );

        } finally {
            if (button.isConnected) {
                button.disabled = false;
            }
        }
    });

}
