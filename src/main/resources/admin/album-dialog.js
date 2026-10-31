const albumDialog = document.getElementById("album-dialog");
const albumForm = document.getElementById("album-form");
const albumTitleInput = document.getElementById("album-title");
const albumDateInput = document.getElementById("album-release-date");
const albumError = document.getElementById("album-error");

export function requestAlbumDetails() {
    return new Promise(resolve => {
        albumForm.reset();
        albumError.hidden = true;

        function onSubmit(event) {
            event.preventDefault();

            const title = albumTitleInput.value.trim();

            if (!title) {
                albumError.textContent = "Название не должно быть пустым";
                albumError.hidden = false;
                return;
            }

            cleanup();
            albumDialog.close();

            resolve({
                title,
                releaseDate: albumDateInput.value || null
            });
        }

        function onClose() {
            cleanup();
            resolve(null);
        }

        function cleanup() {
            albumForm.removeEventListener("submit", onSubmit);
            albumDialog.removeEventListener("close", onClose);
        }

        albumForm.addEventListener("submit", onSubmit);
        albumDialog.addEventListener("close", onClose);

        albumDialog.showModal();
        albumTitleInput.focus();
    });
}

export function initAlbumDialog() {
    document.getElementById("cancel-album-btn")
        .addEventListener("click", () => albumDialog.close());

    document.getElementById("cancel-album-footer")
        .addEventListener("click", () => albumDialog.close());
}
