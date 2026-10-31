-- запрещаю удалять исполнителя
-- если он используется хотя бы одним треком

ALTER TABLE track_artists DROP CONSTRAINT track_artists_artist_id_fkey;

ALTER TABLE track_artists
    ADD CONSTRAINT track_artists_artist_id_fk
        FOREIGN KEY (artist_id)
            REFERENCES artists(id)
            ON DELETE RESTRICT;


-- запрещаю удалять альбом
-- если он используется хотя бы одним треком

ALTER TABLE track_albums DROP CONSTRAINT track_albums_album_id_fkey;

ALTER TABLE track_albums
    ADD CONSTRAINT track_albums_album_id_fk
        FOREIGN KEY (album_id)
            REFERENCES albums(id)
            ON DELETE RESTRICT;


-- запрещаю удалять жанр
-- если он используется хотя бы одним треком

ALTER TABLE track_genres DROP CONSTRAINT track_genres_genre_id_fkey;

ALTER TABLE track_genres
    ADD CONSTRAINT track_genres_genre_id_fk
        FOREIGN KEY (genre_id)
            REFERENCES genres(id)
            ON DELETE RESTRICT;