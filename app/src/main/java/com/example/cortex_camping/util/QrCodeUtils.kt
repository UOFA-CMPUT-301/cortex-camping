package com.example.cortex_camping.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

object QrCodeUtils {
    const val PREVIEW_SIZE_PX = 720
    private const val SAVED_SIZE_PX = 2048 // prints sharply at about 7 inches

    /** Encodes [content] as a square QR code, including the white quiet zone scanners need. */
    fun encode(content: String, sizePx: Int): BitMatrix =
        QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, mapOf(EncodeHintType.MARGIN to 4))

    /** Always black on white, even in dark mode, because inverted codes often fail to scan. */
    fun bitmap(content: String, sizePx: Int): Bitmap {
        val matrix = encode(content, sizePx)
        val pixels = IntArray(sizePx * sizePx) { i ->
            if (matrix[i % sizePx, i / sizePx]) Color.BLACK else Color.WHITE
        }
        return Bitmap.createBitmap(pixels, sizePx, sizePx, Bitmap.Config.ARGB_8888)
    }

    /** A file name for [campsiteName]'s QR code, with anything unsafe in a file name replaced. */
    fun fileName(campsiteName: String): String {
        val safeName = campsiteName.trim().replace(Regex("[^\\p{L}\\p{N}]+"), "-").trim('-')
        return if (safeName.isEmpty()) "campsite-qr-code.png" else "$safeName-qr-code.png"
    }

    /** Writes a print-quality QR code of [content] to [uri] as a PNG. Throws if that fails. */
    suspend fun savePng(context: Context, content: String, uri: Uri) = withContext(Dispatchers.IO) {
        val bitmap = bitmap(content, SAVED_SIZE_PX)
        val stream = context.contentResolver.openOutputStream(uri)
            ?: throw IOException("Couldn't open $uri")
        stream.use {
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) throw IOException("Couldn't write $uri")
        }
    }
}
