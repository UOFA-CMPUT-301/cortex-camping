package com.example.cortex_camping.util

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class ImageUtilsTest {
    @Test
    fun `decodes photos that predate Firebase Storage`() {
        val jpeg = byteArrayOf(1, 2, 3)
        val uri = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(jpeg)
        assertArrayEquals(jpeg, ImageUtils.decodeDataUri(uri))
    }

    @Test
    fun `anything that isn't a valid data URI isn't decoded`() {
        assertNull(ImageUtils.decodeDataUri(null))
        assertNull(ImageUtils.decodeDataUri("https://firebasestorage.googleapis.com/v0/b/bucket/o/a.jpg"))
        assertNull(ImageUtils.decodeDataUri("data:image/jpeg;base64,%%%"))
    }

    @Test
    fun `only picked photos are local`() {
        assertTrue(ImageUtils.isLocalUri("content://media/picker/1"))
        assertFalse(ImageUtils.isLocalUri("https://firebasestorage.googleapis.com/v0/b/bucket/o/a.jpg"))
        assertFalse(ImageUtils.isLocalUri(null))
    }
}
