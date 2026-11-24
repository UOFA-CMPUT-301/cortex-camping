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

/** Prepares picked photos for upload to Firebase Storage. */
object ImageUtils {
    private const val MAX_DIMENSION = 1600
    private const val JPEG_QUALITY = 85

    // Photos saved before Firebase Storage was set up are data URIs inside the campsite document
    private const val DATA_URI_PREFIX = "data:image/jpeg;base64,"

    fun isLocalUri(image: String?): Boolean =
        image != null && (image.startsWith("content://") || image.startsWith("file://"))

    /** Shrinks the photo at [uriString] and returns it as a JPEG. Throws if it can't be read. */
    suspend fun encodeAsJpeg(context: Context, uriString: String): ByteArray =
        compress(load(context, uriString))

    /** The JPEG bytes of a data URI, or null if [image] is anything else. */
    fun decodeDataUri(image: String?): ByteArray? {
        if (image == null || !image.startsWith(DATA_URI_PREFIX)) return null
        return runCatching { Base64.getDecoder().decode(image.removePrefix(DATA_URI_PREFIX)) }.getOrNull()
    }

    // Coil applies the photo's EXIF rotation and downsamples while decoding
    private suspend fun load(context: Context, uriString: String): Bitmap {
        val request = ImageRequest.Builder(context)
            .data(uriString)
            .size(MAX_DIMENSION)
            .precision(Precision.INEXACT)
            .allowHardware(false)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .build()
        val result = context.imageLoader.execute(request)
        if (result is ErrorResult) throw result.throwable
        return (result.drawable as BitmapDrawable).bitmap
    }

    private suspend fun compress(bitmap: Bitmap): ByteArray = withContext(Dispatchers.Default) {
        // JPEG has no alpha, and transparent pixels would otherwise turn black
        val opaque = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Canvas(opaque).apply {
            drawColor(Color.WHITE)
            drawBitmap(bitmap, 0f, 0f, null)
        }
        val out = ByteArrayOutputStream()
        check(opaque.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)) { "Couldn't encode the image" }
        out.toByteArray()
    }
}
