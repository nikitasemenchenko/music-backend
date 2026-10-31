// Точка входа: подключает обработчики только один раз.
import { initTracks, loadTracks } from "./tracks.js";
import { initTrackForm, openTrackForm } from "./track-form.js";
import { initReferences } from "./references.js";
import { initAlbumDialog } from "./album-dialog.js";

initAlbumDialog();
initReferences();
initTrackForm();
initTracks(openTrackForm);

loadTracks();
