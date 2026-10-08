package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

object ImageUtils {

    /**
     * Resolves an imageUrl string into an object that Coil can render:
     * - Base64 data URLs ("data:image/...") are decoded into ByteArray
     * - Web URLs ("http://", "https://") are returned directly
     * - Content URIs ("content://") are parsed into android.net.Uri
     * - Blank/null returns a default homemade dish image
     */
    fun resolveImageModel(imageUrl: String?): Any {
        if (imageUrl.isNullOrBlank()) {
            return "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=600"
        }
        val trimmed = imageUrl.trim()
        if (trimmed.startsWith("data:image") && trimmed.contains("base64,")) {
            return try {
                val base64Data = trimmed.substringAfter("base64,")
                Base64.decode(base64Data, Base64.DEFAULT)
            } catch (e: Exception) {
                trimmed
            }
        }
        if (trimmed.startsWith("content://")) {
            return try {
                Uri.parse(trimmed)
            } catch (_: Exception) {
                trimmed
            }
        }
        return trimmed
    }

    /**
     * Resizes and compresses an image from the device gallery into a Base64 JPEG string.
     * Stored directly in Firebase Firestore so it persists across logout/login and across devices.
     */
    fun uriToBase64(context: Context, uri: Uri, maxDimension: Int = 600, quality: Int = 75): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (originalBitmap == null) return null

            val width = originalBitmap.width
            val height = originalBitmap.height
            val scale = if (width > height) {
                if (width > maxDimension) maxDimension.toFloat() / width else 1f
            } else {
                if (height > maxDimension) maxDimension.toFloat() / height else 1f
            }

            val scaledBitmap = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    originalBitmap,
                    (width * scale).toInt().coerceAtLeast(1),
                    (height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                originalBitmap
            }

            val stream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            val bytes = stream.toByteArray()
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64"
        } catch (e: Exception) {
            android.util.Log.e("HomeChef", "Error converting image to Base64: ${e.message}", e)
            null
        }
    }
}
