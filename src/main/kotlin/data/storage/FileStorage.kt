package ru.magnum.data.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path

class FileStorage(
    settings: StorageSettings
) {
    private val root: Path = Path.of(settings.root).toAbsolutePath().normalize()

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
}