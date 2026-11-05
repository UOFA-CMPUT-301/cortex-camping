package com.example.cortex_camping.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.size.Precision
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Base64

/**
 * Campsite photos are stored inside the Firestore campsite document as a JPEG data URI, because
 * this project has no Firebase Storage bucket. Firestore documents are capped at 1 MiB and every
 * campsite is downloaded at startup, so photos are kept well under that.
 */
object ImageUtils {
    private const val DATA_URI_PREFIX = "data:image/jpeg;base64,"
    private const val MAX_JPEG_BYTES = 400 * 1024 // about 530 KB once base64 encoded
    private val MAX_DIMENSIONS = listOf(1024, 768, 512)
    private val JPEG_QUALITIES = listOf(80, 60, 40)

    fun isLocalUri(image: String?): Boolean =
        image != null && (image.startsWith("content://") || image.startsWith("file://"))

    /** Shrinks the photo at [uriString] and returns it as a data URI. Throws if it can't be read. */
    suspend fun encodeAsDataUri(context: Context, uriString: String): String {
        for (dimension in MAX_DIMENSIONS) {
            val jpeg = compress(load(context, uriString, dimension))
            if (jpeg != null) return DATA_URI_PREFIX + Base64.getEncoder().encodeToString(jpeg)
        }
        throw IllegalArgumentException("Couldn't shrink the image below $MAX_JPEG_BYTES bytes")
    }

    /** The JPEG bytes of a data URI, or null if [image] is anything else. */
    fun decodeDataUri(image: String?): ByteArray? {
        if (image == null || !image.startsWith(DATA_URI_PREFIX)) return null
        return runCatching { Base64.getDecoder().decode(image.removePrefix(DATA_URI_PREFIX)) }.getOrNull()
    }

    // Coil applies the photo's EXIF rotation and downsamples while decoding
    private suspend fun load(context: Context, uriString: String, maxDimension: Int): Bitmap {
        val request = ImageRequest.Builder(context)
            .data(uriString)
            .size(maxDimension)
            .precision(Precision.INEXACT)
            .allowHardware(false)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .build()
        val result = context.imageLoader.execute(request)
        if (result is ErrorResult) throw result.throwable
        return (result.drawable as BitmapDrawable).bitmap
    }

    private suspend fun compress(bitmap: Bitmap): ByteArray? = withContext(Dispatchers.Default) {
        // JPEG has no alpha, and transparent pixels would otherwise turn black
        val opaque = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Canvas(opaque).apply {
            drawColor(Color.WHITE)
            drawBitmap(bitmap, 0f, 0f, null)
        }
        JPEG_QUALITIES.firstNotNullOfOrNull { quality ->
            val out = ByteArrayOutputStream()
            opaque.compress(Bitmap.CompressFormat.JPEG, quality, out)
            out.toByteArray().takeIf { it.size <= MAX_JPEG_BYTES }
        }
    }
}
