/**
 * Tests CampsiteImageRepository against Firebase Storage: uploading, downloading by URL, deleting.
 */

package com.example.cortex_camping

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cortex_camping.repository.CampsiteImageRepository
import com.example.cortex_camping.util.ImageUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class CampsiteImageRepositoryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val campsiteId = "test-campsite-${UUID.randomUUID()}"
    private val repo = CampsiteImageRepository()
    private val uploaded = mutableListOf<String>()

    private suspend fun upload(jpeg: ByteArray) = repo.upload(campsiteId, jpeg).also { uploaded += it }

    private fun open(url: String) = URL(url).openConnection() as HttpURLConnection

    private suspend fun awaitGone(url: String) = withTimeout(20_000) {
        while (open(url).responseCode != 404) delay(500)
    }

    // make sure that no photos are left after testing
    @After
    fun deleteTestPhotos() = runBlocking {
        uploaded.forEach { repo.delete(it) }
        uploaded.forEach { awaitGone(it) }
    }

    private fun jpeg(width: Int = 8, height: Int = 8): ByteArray = ByteArrayOutputStream().also {
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.JPEG, 90, it)
    }.toByteArray()

    @Test
    fun uploadedPhotoIsServedFromItsUrl() = runBlocking {
        val photo = jpeg()
        val connection = open(upload(photo))
        assertEquals(200, connection.responseCode)
        assertEquals("image/jpeg", connection.contentType)
        assertArrayEquals(photo, connection.inputStream.use { it.readBytes() })
    }

    @Test
    fun replacingAPhotoGivesANewUrlAndKeepsItAfterTheOldOneIsDeleted() = runBlocking {
        val first = upload(jpeg())
        val second = upload(jpeg())
        assertNotEquals(first, second)

        repo.delete(first)
        awaitGone(first)
        assertEquals(200, open(second).responseCode)
    }

    @Test
    fun valuesThatAreNotStorageFilesAreIgnoredByDelete() {
        listOf(null, "", "data:image/jpeg;base64,AQID", "https://example.com/a.jpg", "gs://other-bucket/a.jpg")
            .forEach { repo.delete(it) }
    }

    @Test
    fun pickedPhotosAreShrunkBeforeUpload() = runBlocking {
        val file = File(context.cacheDir, "$campsiteId.jpg").apply { writeBytes(jpeg(3200, 2400)) }
        try {
            val encoded = ImageUtils.encodeAsJpeg(context, "file://${file.path}")
            val bitmap = BitmapFactory.decodeByteArray(encoded, 0, encoded.size)
            assertTrue("${bitmap.width}x${bitmap.height}", bitmap.width <= 1600 && bitmap.height <= 1600)
        } finally {
            file.delete()
        }
    }
}
