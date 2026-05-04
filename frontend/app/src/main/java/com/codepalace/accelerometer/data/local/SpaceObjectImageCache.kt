package com.codepalace.accelerometer.data.local

import android.content.Context
import com.codepalace.accelerometer.config.ApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

class SpaceObjectImageCache(context: Context) {
    private val appContext = context.applicationContext
    private val directory = File(appContext.filesDir, "space_object_images")
    private val client = OkHttpClient()

    fun getImage(spaceObjectId: Long): File? {
        val file = imageFile(spaceObjectId)
        return file.takeIf { it.exists() && it.length() > 0L }
    }

    fun deleteImage(spaceObjectId: Long) {
        runCatching {
            imageFile(spaceObjectId).delete()
        }
    }

    suspend fun downloadAndSave(spaceObjectId: Long, imageUrl: String): File? {
        return withContext(Dispatchers.IO) {
            runCatching {
                if (!directory.exists()) {
                    directory.mkdirs()
                }

                val request = Request.Builder()
                    .url(absoluteUrl(imageUrl))
                    .build()

                client.newCall(request).execute().use { response ->
                    val body = response.body ?: return@withContext null
                    val contentType = body.contentType()
                    if (!response.isSuccessful ||
                        (contentType != null && !contentType.type.equals("image", ignoreCase = true))
                    ) {
                        return@withContext null
                    }

                    val bytes = body.bytes().takeIf { it.isNotEmpty() }
                        ?: return@withContext null

                    val file = imageFile(spaceObjectId)
                    val tempFile = File(directory, "${file.name}.tmp")
                    tempFile.writeBytes(bytes)

                    if (!tempFile.renameTo(file)) {
                        file.writeBytes(bytes)
                        tempFile.delete()
                    }

                    file
                }
            }.getOrNull()
        }
    }

    private fun imageFile(spaceObjectId: Long): File {
        return File(directory, "space_object_$spaceObjectId")
    }

    private fun absoluteUrl(path: String): String {
        return if (path.startsWith("http", ignoreCase = true)) {
            path
        } else {
            ApiConfig.BASE_URL.trimEnd('/') + "/" + path.trimStart('/')
        }
    }
}
