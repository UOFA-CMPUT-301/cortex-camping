package com.example.cortex_camping.util

import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Test

class QrCodeUtilsTest {
    @Test
    fun campsiteLinkSurvivesEncodeAndScan() {
        val link = "cortexcamping://campsite/3f2b8c1e-7a44-4d0e-9d55-1c2e6f0a9b77"
        val size = 2048
        val matrix = QrCodeUtils.encode(link, size)
        val pixels = IntArray(size * size) { i ->
            if (matrix[i % size, i / size]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
        val image = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(size, size, pixels)))

        assertEquals(link, QRCodeReader().decode(image).text)
    }

    @Test
    fun fileNameReplacesUnsafeCharacters() {
        assertEquals("Pine-Forest-Escape-qr-code.png", QrCodeUtils.fileName(" Pine/Forest: Escape! "))
        assertEquals("campsite-qr-code.png", QrCodeUtils.fileName("???"))
    }
}
