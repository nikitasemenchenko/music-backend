
package ru.magnum.api.routes

import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveMultipart
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.magnum.data.storage.FileStorage
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

private const val MAX_AUDIO_BYTES = 100L * 1024 * 1024
private const val MAX_COVER_BYTES = 5L * 1024 * 1024

private val COVER_EXTENSIONS = setOf(
    "jpg",
    "jpeg",
    "png",
    "webp"
)

data class TrackMultipartData(
    val metadataJson: String?,
    val audioTempFile: Path?,
    val audioExtension: String?,
    val coverTempFile: Path?,
    val coverExtension: String?
)

suspend fun ApplicationCall.receiveTrackMultipart(
    fileStorage: FileStorage
): TrackMultipartData {
    var metadataJson: String? = null
    var audioTempFile: Path? = null
    var audioExtension: String? = null
    var coverTempFile: Path? = null
    var coverExtension: String? = null

    try {
        receiveMultipart().forEachPart { part ->
            try {
                when (part) {
                    is PartData.FormItem -> {
                        if (part.name == "metadata") {
                            require(metadataJson == null) {
                                "Metadata must be provided only once"
                            }

                            metadataJson = part.value
                        }
                    }

                    is PartData.FileItem -> {
                        when (part.name) {
                            "audio" -> {
                                require(audioTempFile == null) {
                                    "Only one audio file is allowed"
                                }

                                val extension = fileExtension(part.originalFileName)

                                require(extension == "mp3") {
                                    "Audio file must have an MP3 extension"
                                }

                                val tempFile = fileStorage.createTempFile(extension)

                                try {
                                    val header = saveFileAndGetHeader(
                                        source = part.provider(),
                                        destination = tempFile,
                                        maxBytes = MAX_AUDIO_BYTES
                                    )

                                    require(isValidMp3(header)) {
                                        "Audio file is not a valid MP3 file"
                                    }

                                    audioTempFile = tempFile
                                    audioExtension = extension
                                } catch (e: Exception) {
                                    deleteTempAfterFail(
                                        fileStorage,
                                        tempFile,
                                        e
                                    )
                                    throw e
                                }
                            }

                            "cover" -> {
                                require(coverTempFile == null) {
                                    "Only one cover file is allowed"
                                }

                                val extension = fileExtension(part.originalFileName)

                                require(extension in COVER_EXTENSIONS) {
                                    "Cover must be JPG, JPEG, PNG or WEBP"
                                }

                                val tempFile = fileStorage.createTempFile(extension)

                                try {
                                    val header = saveFileAndGetHeader(
                                        source = part.provider(),
                                        destination = tempFile,
                                        maxBytes = MAX_COVER_BYTES
                                    )

                                    require(
                                        hasValidImageType(
                                            extension,
                                            header
                                        )
                                    ) {
                                        "Cover content does not match its file extension"
                                    }

                                    coverTempFile = tempFile
                                    coverExtension = extension
                                } catch (e: Exception) {
                                    deleteTempAfterFail(
                                        fileStorage,
                                        tempFile,
                                        e
                                    )
                                    throw e
                                }
                            }
                        }
                    }

                    else -> Unit
                }
            } finally {
                part.release()
            }
        }

        return TrackMultipartData(
            metadataJson = metadataJson,
            audioTempFile = audioTempFile,
            audioExtension = audioExtension,
            coverTempFile = coverTempFile,
            coverExtension = coverExtension
        )
    } catch (e: Exception) {
        audioTempFile?.let {
            deleteTempAfterFail(fileStorage, it, e)
        }

        coverTempFile?.let {
            deleteTempAfterFail(fileStorage, it, e)
        }

        throw e
    }
}

private suspend fun saveFileAndGetHeader(
    source: ByteReadChannel,
    destination: Path,
    maxBytes: Long
): ByteArray = withContext(Dispatchers.IO) {
    val header = ByteArray(12)
    var headerSize = 0
    var totalBytes = 0L
    val buffer = ByteArray(8192)

    Files.newOutputStream(
        destination,
        StandardOpenOption.WRITE,
        StandardOpenOption.TRUNCATE_EXISTING
    ).use { output ->
        while (true) {
            val count = source.readAvailable(
                buffer,
                0,
                buffer.size
            )

            if (count == -1) break
            if (count == 0) continue

            totalBytes += count

            require(totalBytes <= maxBytes) {
                "Out of ${maxBytes / (1024 * 1024)} MB"
            }

            val headerCount = minOf(
                count,
                header.size - headerSize
            )

            if (headerCount > 0) {
                buffer.copyInto(
                    destination = header,
                    destinationOffset = headerSize,
                    startIndex = 0,
                    endIndex = headerCount
                )

                headerSize += headerCount
            }

            output.write(buffer, 0, count)
        }
    }

    require(totalBytes > 0) {
        "Must not be empty"
    }

    header.copyOf(headerSize)
}

private fun isValidMp3(header: ByteArray): Boolean {
    val hasId3Header =
        header.size >= 3 &&
                header[0] == 'I'.code.toByte() &&
                header[1] == 'D'.code.toByte() &&
                header[2] == '3'.code.toByte()

    val hasMpegFrame =
        header.size >= 2 &&
                (header[0].toInt() and 0xFF) == 0xFF &&
                (header[1].toInt() and 0xE0) == 0xE0

    return hasId3Header || hasMpegFrame
}

private fun hasValidImageType(
    extension: String,
    header: ByteArray
): Boolean {
    return when (extension) {
        "jpg", "jpeg" ->
            header.size >= 3 &&
                    header[0] == 0xFF.toByte() &&
                    header[1] == 0xD8.toByte() &&
                    header[2] == 0xFF.toByte()

        "png" -> {
            val signature = byteArrayOf(
                0x89.toByte(),
                0x50,
                0x4E,
                0x47,
                0x0D,
                0x0A,
                0x1A,
                0x0A
            )

            header.size >= signature.size &&
                    signature.indices.all { header[it] == signature[it] }
        }

        "webp" ->
            header.size >= 12 &&
                    header.copyOfRange(0, 4).contentEquals(
                        byteArrayOf(0x52, 0x49, 0x46, 0x46)
                    ) &&
                    header.copyOfRange(8, 12).contentEquals(
                        byteArrayOf(0x57, 0x45, 0x42, 0x50)
                    )

        else -> false
    }
}

private suspend fun deleteTempAfterFail(
    fileStorage: FileStorage,
    path: Path,
    originalException: Exception
) {
    try {
        fileStorage.deleteTemp(path)
    } catch (cleanException: Exception) {
        originalException.addSuppressed(cleanException)
    }
}

private fun fileExtension(fileName: String?): String {
    if (fileName.isNullOrBlank()) {
        return ""
    }

    return fileName.substringAfterLast('.', "").lowercase()
}
