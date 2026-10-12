package ru.magnum.data.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

class FileStorage(
    settings: StorageSettings
) {
    private val root: Path = Path.of(settings.root).toAbsolutePath().normalize()
    private val tempRoot: Path = root.resolve("tmp").normalize()

    suspend fun read(
        relativePath: String
    ): ByteArray? {
        return withContext(Dispatchers.IO) {
            val filePath = root.resolve(relativePath).normalize()
            if(!filePath.startsWith(root)){
                return@withContext null
            }
            if(!Files.isRegularFile(filePath)){
                return@withContext null
            }
            Files.readAllBytes(filePath)
        }
    }

    suspend fun getPath(
        relativePath: String
    ): Path? {
        return withContext(Dispatchers.IO){
            val filePath = root.resolve(relativePath).normalize()
            if(!filePath.startsWith(root)){
                return@withContext null
            }
            if(!Files.isRegularFile(filePath)){
                return@withContext null
            }
            filePath
        }
    }

    suspend fun delete(
        relativePath: String
    ) {
        withContext(Dispatchers.IO) {
            val filePath = root.resolve(relativePath).normalize()
            if(!filePath.startsWith(root)){
                return@withContext
            }
            Files.deleteIfExists(filePath)
        }
    }

    suspend fun deleteTemp(
        path: Path
    ){
        withContext(Dispatchers.IO) {
            val normalizePath = path.toAbsolutePath().normalize()
            if(!normalizePath.startsWith(root)){
                return@withContext
            }
            Files.deleteIfExists(normalizePath)
        }
    }

    private fun validateExtension(
        extension: String
    ) {
        require(
            extension.matches(
                Regex("[a-zA-Z0-9]+")
            )
        ) {
            "Invalid extension"
        }
    }

    suspend fun createTempFile(
        extension: String
    ): Path {
        validateExtension(extension)
        return withContext(Dispatchers.IO) {
            Files.createDirectories(tempRoot)
            Files.createTempFile(
                tempRoot,
                "upload-",
                ".$extension"
            )
        }
    }

    suspend fun moveTempFile(
        tempFile: Path,
        directory: String,
        extension: String
    ): String {
        validateExtension(extension)

        return withContext(Dispatchers.IO) {
            val normalizedTemp = tempFile.toAbsolutePath().normalize()
            if(!normalizedTemp.startsWith(tempRoot)){
                throw IllegalArgumentException(
                    "Invalid temporary file"
                )
            }
            if(!Files.isRegularFile(normalizedTemp)){
                throw IllegalArgumentException(
                    "Temporary file doesn't exist"
                )
            }
            val targetDirectory = root.resolve(directory).normalize()
            if(!targetDirectory.startsWith(root)){
                throw IllegalArgumentException(
                    "Invalid storage directory"
                )
            }
            Files.createDirectories(targetDirectory)
            val fileName = "${UUID.randomUUID()}.$extension"
            val targetFile = targetDirectory.resolve(fileName).normalize()
            Files.move(
                normalizedTemp,
                targetFile
            )
            root.relativize(targetFile).toString().replace("\\", "/")
        }
    }
}