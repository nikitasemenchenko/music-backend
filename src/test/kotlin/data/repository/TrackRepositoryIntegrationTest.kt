package data.repository

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.insert
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import ru.magnum.data.database.DatabaseFactory
import ru.magnum.data.database.DatabaseSettings
import ru.magnum.data.database.MigrationFactory
import ru.magnum.data.repository.TrackAdminRepository
import ru.magnum.data.repository.TrackRepositoryImpl
import ru.magnum.data.table.AlbumsTable
import ru.magnum.data.table.ArtistsTable
import ru.magnum.data.table.GenresTable
import ru.magnum.domain.model.CreateTrack
import ru.magnum.domain.model.UpdateTrack
import java.time.LocalDate
import java.util.UUID
import kotlin.getValue
import kotlin.test.*

class TrackRepositoryIntegrationTest {

    companion object {
        private val postgres = PostgreSQLContainer<Nothing>("postgres:16-alpine")
                .apply {
                    withDatabaseName("music_test")
                    withUsername("test_user")
                    withPassword("test_password")
                }

        private lateinit var databaseFactory: DatabaseFactory

        @BeforeAll
        @JvmStatic
        fun setUp() {
            postgres.start()

            val settings = DatabaseSettings(
                jdbcUrl = postgres.jdbcUrl,
                username = postgres.username,
                password = postgres.password,
                driverClassName = "org.postgresql.Driver",
                maximumPoolSize = 3
            )

            MigrationFactory.migrate(settings)
            databaseFactory = DatabaseFactory(settings)
        }

        @AfterAll
        @JvmStatic
        fun drop() {
            if (::databaseFactory.isInitialized) {
                databaseFactory.close()
            }

            postgres.stop()
        }
    }

    private val adminRepository by lazy {
        TrackAdminRepository(databaseFactory)
    }

    private val trackRepository by lazy {
        TrackRepositoryImpl(databaseFactory)
    }

    private suspend fun createTestMetadata(): Triple<Int, Int, Int> {
        return databaseFactory.dbQuery {
            val x = UUID.randomUUID().toString()

            val artistId = ArtistsTable.insert {
                it[ArtistsTable.name] = "Test Artist $x"
            } get ArtistsTable.id

            val albumId = AlbumsTable.insert {
                it[AlbumsTable.title] = "Test Album $x"
            } get AlbumsTable.id

            val genreId = GenresTable.insert {
                it[GenresTable.name] = "Test Genre $x"
            } get GenresTable.id

            Triple(artistId, albumId, genreId)
        }
    }

    @Test
    fun `create read update and delete track`() = runBlocking {
        val (artistId, albumId, genreId) =
            createTestMetadata()

        val audioPath =
            "audio/test-${UUID.randomUUID()}.mp3"

        // CREATE
        val id = adminRepository.createTrack(
            track = CreateTrack(
                title = "Original Title",
                artistIds = listOf(artistId),
                albumIds = listOf(albumId),
                genreIds = listOf(genreId),
                durationMs = 180000,
                releaseDate = LocalDate.of(2026, 10, 22)
            ),
            audioPath = audioPath,
            coverPath = null
        )

        assertTrue(id > 0)

        // READ
        val created = trackRepository.getTrackById(id)

        assertNotNull(created)
        assertEquals("Original Title", created.title)
        assertEquals(180000, created.durationMs)
        assertEquals(listOf(artistId), created.artists.map { it.id })
        assertEquals(listOf(albumId), created.albums.map { it.id })
        assertEquals(listOf(genreId), created.genres.map { it.id })

        // UPDATE
        val oldPaths = adminRepository.updateTrack(
            id = id,
            track = UpdateTrack(
                title = "Updated Title",
                artistIds = listOf(artistId),
                albumIds = emptyList(),
                genreIds = emptyList(),
                durationMs = 200000,
                releaseDate = null
            ),
            newAudioPath = null,
            newCoverPath = null
        )

        assertNotNull(oldPaths)
        assertEquals(audioPath, oldPaths.audioPath)

        val updated = trackRepository.getTrackById(id)

        assertNotNull(updated)
        assertEquals("Updated Title", updated.title)
        assertEquals(200000, updated.durationMs)
        assertTrue(updated.albums.isEmpty())
        assertTrue(updated.genres.isEmpty())
        assertNull(updated.releaseDate)

        // DELETE
        val deleted = adminRepository.deleteTrack(id)

        assertNotNull(deleted)
        assertEquals(audioPath, deleted.audioPath)

        assertNull(trackRepository.getTrackById(id))
    }

    @Test
    fun `exception when artist dont exist`() = runBlocking {
        val error = runCatching {
            adminRepository.createTrack(
                track = CreateTrack(
                    title = "Invalid Track",
                    artistIds = listOf(99999999),
                    albumIds = emptyList(),
                    genreIds = emptyList(),
                    durationMs = 150000,
                    releaseDate = null
                ),
                audioPath = "audio/${UUID.randomUUID()}.mp3",
                coverPath = null
            )
        }.exceptionOrNull()

        assertIs<IllegalArgumentException>(error)
    }
}