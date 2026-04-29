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
                    if (!response.isSuccessful) return@withContext null

                    val bytes = response.body?.bytes()?.takeIf { it.isNotEmpty() }
                        ?: return@withContext null

                    imageFile(spaceObjectId).also { file ->
                        file.writeBytes(bytes)
                    }
                }
            }.getOrNull()
        }
    }

    private fun imageFile(spaceObjectId: Long): File {
        return File(directory, "space_object_$spaceObjectId")
    }

    private fun absoluteUrl(path: String): String {
        return if (path.startsWith("http")) {
            path
        } else {
            ApiConfig.BASE_URL.trimEnd('/') + "/" + path.trimStart('/')
        }
    }
}
