package api

import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.testcontainers.containers.PostgreSQLContainer
import ru.magnum.module
import java.nio.file.Files
import java.nio.file.Path
import java.sql.DriverManager
import java.util.Base64
import java.util.UUID
import kotlin.test.*

class TrackApiIntegrationTest {

    companion object {
        private val postgres =
            PostgreSQLContainer<Nothing>("postgres:16-alpine")
                .apply {
                    withDatabaseName("music_api_test")
                    withUsername("test_user")
                    withPassword("test_password")
                }

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            postgres.start()
        }

        @AfterAll
        @JvmStatic
        fun afterAll() {
            postgres.stop()
        }
    }

    private fun ApplicationTestBuilder.configureMusicApp(
        storageRoot: Path
    ) {
        environment {
            config = MapApplicationConfig(
                "database.jdbcUrl" to postgres.jdbcUrl,
                "database.username" to postgres.username,
                "database.password" to postgres.password,
                "database.driverClassName" to "org.postgresql.Driver",
                "database.maximumPoolSize" to "3",
                "storage.root" to storageRoot.toString()
            )
        }

        application {
            module()
        }
    }

    private fun createTestMetadata(): Triple<Int, Int, Int> {
        DriverManager.getConnection(
            postgres.jdbcUrl,
            postgres.username,
            postgres.password
        ).use { connection ->

            fun insertData(
                table: String,
                column: String,
                value: String
            ): Int {
                connection.prepareStatement(
                    "INSERT INTO $table ($column) VALUES (?) RETURNING id"
                ).use { statement ->
                    statement.setString(1, value)

                    statement.executeQuery().use { result ->
                        check(result.next())
                        return result.getInt(1)
                    }
                }
            }

            val x = UUID.randomUUID().toString()

            val artistId = insertData(
                "artists", "name", "Test Artist $x"
            )

            val albumId = insertData(
                "albums", "title", "Test Album $x"
            )

            val genreId = insertData(
                "genres", "name", "Test Genre $x"
            )

            return Triple(artistId, albumId, genreId)
        }
    }

    @Test
    fun `track CRUD and streaming through HTTP`(
        @TempDir storageRoot: Path
    ) = testApplication {

        configureMusicApp(storageRoot)

        // Запускаем приложение включая Flyway и Koin
        startApplication()

        val (artistId, albumId, genreId) = createTestMetadata()

        // Небольшой тестовый набор байтов с ID3 сигнатурой
        // Это не полноценный мп3
        val audio = "ID3".toByteArray() + ByteArray(256)

        // Маленький PNG 1х1 для проверки загрузки обложки
        val cover = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwC" + "AAAAC0lEQVR42mP8/x8AAusB9W+lE64AAAAASUVORK5CYII="
        )

        val createMetadata = """
    {
        "title": "Test Track",
        "artist_ids": [$artistId],
        "album_ids": [$albumId],
        "genre_ids": [$genreId],
        "duration_ms": 180000,
        "release_date": "2026-05-10"
    }
""".trimIndent()

        // 1. CREATE
        val created = client.submitFormWithBinaryData(
            url = "/api/v1/admin/tracks",
            formData = formData {
                append("metadata", createMetadata)

                append(
                    "audio",
                    audio,
                    Headers.build {
                        append(HttpHeaders.ContentType, "audio/mpeg")
                        append(
                            HttpHeaders.ContentDisposition,
                            "filename=\"test.mp3\""
                        )
                    }
                )

                append(
                    "cover",
                    cover,
                    Headers.build {
                        append(HttpHeaders.ContentType, "image/png")
                        append(
                            HttpHeaders.ContentDisposition,
                            "filename=\"cover.png\""
                        )
                    }
                )
            }
        )

        assertEquals(HttpStatusCode.Created, created.status)

        val id = Json.parseToJsonElement(
            created.bodyAsText()
        ).jsonObject.getValue("id").jsonPrimitive.int

        // READ
        val received = client.get("/api/v1/tracks/$id")

        assertEquals(HttpStatusCode.OK, received.status)

        val trackJson = Json.parseToJsonElement(
            received.bodyAsText()
        ).jsonObject

        assertEquals(
            "Test Track",
            trackJson.getValue("title").jsonPrimitive.content
        )

        assertEquals(
            artistId,
            trackJson.getValue("artists")
                .jsonArray.single()
                .jsonObject.getValue("id")
                .jsonPrimitive.int
        )

        // нельзя удалять исполнителя используемого треком
        val deleteArtist = client.delete(
            "/api/v1/admin/references/artists/$artistId"
        )

        assertEquals(
            HttpStatusCode.Conflict,
            deleteArtist.status
        )

        // нельзя удалять альбом используемый треком
        val deleteAlbum = client.delete(
            "/api/v1/admin/references/albums/$albumId"
        )

        assertEquals(
            HttpStatusCode.Conflict,
            deleteAlbum.status
        )

        // нельзя удалять жанр используемый треком
        val deleteGenre = client.delete(
            "/api/v1/admin/references/genres/$genreId"
        )

        assertEquals(
            HttpStatusCode.Conflict,
            deleteGenre.status
        )

        assertEquals(
            180000,
            trackJson.getValue("duration_ms").jsonPrimitive.int
        )

        assertEquals(
            "2026-05-10",
            trackJson.getValue("release_date").jsonPrimitive.content
        )

        // COVER
        val receivedCover = client.get(
            "/api/v1/tracks/$id/cover"
        )

        assertEquals(
            HttpStatusCode.OK,
            receivedCover.status
        )

        assertContentEquals(
            cover,
            receivedCover.bodyAsBytes()
        )

        // STREAM + RANGE
        val streamed = client.get(
            "/api/v1/tracks/$id/stream"
        ) {
            header(HttpHeaders.Range, "bytes=0-3")
        }

        assertEquals(
            HttpStatusCode.PartialContent,
            streamed.status
        )

        assertContentEquals(
            audio.copyOfRange(0, 4),
            streamed.bodyAsBytes()
        )

        // UPDATE без замены файлов
        val updateMetadata = """
            {
                "title": "Updated Track",
                "artist_ids": [$artistId],
                "album_ids": [],
                "genre_ids": [$genreId],
                "duration_ms": 200000,
                "release_date": null
            }
            """.trimIndent()

        val updated = client.put(
            "/api/v1/admin/tracks/$id"
        ) {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("metadata", updateMetadata)
                    }
                )
            )
        }

        assertEquals(
            HttpStatusCode.NoContent,
            updated.status
        )

        val afterUpdate = client.get(
            "/api/v1/tracks/$id"
        )

        val updatedJson = Json.parseToJsonElement(
            afterUpdate.bodyAsText()
        ).jsonObject

        assertEquals(
            "Updated Track",
            updatedJson.getValue("title").jsonPrimitive.content
        )

        assertTrue(
            updatedJson.getValue("albums").jsonArray.isEmpty()
        )

        // Проверяем что старые файлы остались
        assertContentEquals(
            cover,
            client.get("/api/v1/tracks/$id/cover")
                .bodyAsBytes()
        )

        assertContentEquals(
            audio,
            client.get("/api/v1/tracks/$id/stream")
                .bodyAsBytes()
        )

        // 6. DELETE
        val deleted = client.delete(
            "/api/v1/admin/tracks/$id"
        )

        assertEquals(
            HttpStatusCode.NoContent,
            deleted.status
        )

        // После удаления запись отсутствует
        val afterDelete = client.get(
            "/api/v1/tracks/$id"
        )

        assertEquals(
            HttpStatusCode.NotFound,
            afterDelete.status
        )

        // После удаления трека его связи тоже должны удалиться

        val deletedArtist = client.delete(
            "/api/v1/admin/references/artists/$artistId"
        )

        assertEquals(
            HttpStatusCode.NoContent,
            deletedArtist.status
        )

        val deletedAlbum = client.delete(
            "/api/v1/admin/references/albums/$albumId"
        )

        assertEquals(
            HttpStatusCode.NoContent,
            deletedAlbum.status
        )

        val deletedGenre = client.delete(
            "/api/v1/admin/references/genres/$genreId"
        )

        assertEquals(
            HttpStatusCode.NoContent,
            deletedGenre.status
        )

        // Физические файлы тоже должны быть удалены
        assertEquals(
            0L,
            Files.list(storageRoot.resolve("audio"))
                .use { it.count() }
        )

        assertEquals(
            0L,
            Files.list(storageRoot.resolve("covers"))
                .use { it.count() }
        )
    }

    @Test
    fun `invalid requests return client errors`(
        @TempDir storageRoot: Path
    ) = testApplication {

        configureMusicApp(storageRoot)
        startApplication()

        // Некорректный ID
        val invalidId = client.get("/api/v1/tracks/0")

        assertEquals(
            HttpStatusCode.BadRequest,
            invalidId.status
        )

        // Несуществующий трек
        val missing = client.get(
            "/api/v1/tracks/99999999"
        )

        assertEquals(
            HttpStatusCode.NotFound,
            missing.status
        )

        // Сломанный JSON в metadata
        val invalidMetadata = client.submitFormWithBinaryData(
            url = "/api/v1/admin/tracks",
            formData = formData {
                append("metadata", """{"title":""")
            }
        )

        assertEquals(
            HttpStatusCode.BadRequest,
            invalidMetadata.status
        )

        // Файл mp3 с неподходящей сигнатурой
        val invalidAudio = client.submitFormWithBinaryData(
            url = "/api/v1/admin/tracks",
            formData = formData {
                append(
                    "metadata",
                    """
                        {
                            "title": "Invalid Audio",
                            "artist_ids": [1],
                            "album_ids": [],
                            "genre_ids": [],
                            "duration_ms": 1000
                        }
                        """.trimIndent()
                )

                append(
                    "audio",
                    byteArrayOf(1, 2, 3, 4),
                    Headers.build {
                        append(
                            HttpHeaders.ContentDisposition,
                            "filename=\"invalid.mp3\""
                        )
                    }
                )
            }
        )

        assertEquals(
            HttpStatusCode.BadRequest,
            invalidAudio.status
        )

        // Несуществующий исполнитель
        val invalidArtist = client.delete(
            "/api/v1/admin/references/artists/99999999"
        )

        assertEquals(
            HttpStatusCode.NotFound,
            invalidArtist.status
        )

        // Некорректный ID жанра
        val invalidGenre = client.delete(
            "/api/v1/admin/references/genres/0"
        )

        assertEquals(
            HttpStatusCode.BadRequest,
            invalidGenre.status
        )
    }
}
