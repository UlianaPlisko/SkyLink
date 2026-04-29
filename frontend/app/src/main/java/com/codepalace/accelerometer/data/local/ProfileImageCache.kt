package com.codepalace.accelerometer.data.local

import android.content.Context
import android.net.Uri
import java.io.File

class ProfileImageCache(context: Context) {
    private val appContext = context.applicationContext
    private val directory = File(appContext.filesDir, "profile_images")

    fun getProfilePicture(userId: Long): File? {
        val file = profilePictureFile(userId)
        return file.takeIf { it.exists() && it.length() > 0L }
    }

    fun saveProfilePicture(userId: Long, bytes: ByteArray): File {
        if (!directory.exists()) {
            directory.mkdirs()
        }

        return profilePictureFile(userId).also { file ->
            file.writeBytes(bytes)
        }
    }

    fun saveProfilePicture(userId: Long, uri: Uri): File? {
        val bytes = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        return bytes?.takeIf { it.isNotEmpty() }?.let { saveProfilePicture(userId, it) }
    }

    private fun profilePictureFile(userId: Long): File {
        val stableUserId = if (userId > 0L) userId else 0L
        return File(directory, "profile_picture_$stableUserId")
    }
}
