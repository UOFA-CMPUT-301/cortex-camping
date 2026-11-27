package com.example.cortex_camping

import com.example.cortex_camping.viewmodel.ProfileImageChange
import org.junit.Assert.assertEquals
import org.junit.Test

/** US 01.08.02: an entrant can add, update, or remove their profile picture. */
class ProfileImageChangeTest {
    private val stored = "https://firebasestorage.googleapis.com/v0/b/bucket/o/entrants%2Fdevice%2Fa.jpg"
    private val picked = "content://media/picker/0/com.android.providers.media.photopicker/media/42"

    @Test
    fun `picking a photo with no picture yet adds it`() {
        assertEquals(ProfileImageChange.Upload(picked), ProfileImageChange.between(null, picked))
    }

    @Test
    fun `picking a photo over the stored picture replaces it`() {
        assertEquals(ProfileImageChange.Upload(picked), ProfileImageChange.between(stored, picked))
    }

    @Test
    fun `removing the stored picture removes it`() {
        assertEquals(ProfileImageChange.Remove, ProfileImageChange.between(stored, null))
    }

    @Test
    fun `leaving the picture alone keeps it`() {
        assertEquals(ProfileImageChange.Keep, ProfileImageChange.between(stored, stored))
        assertEquals(ProfileImageChange.Keep, ProfileImageChange.between(null, null))
    }

    @Test
    fun `a URL that didn't come from the picker isn't saved`() {
        assertEquals(ProfileImageChange.Keep, ProfileImageChange.between(stored, "https://example.com/other.jpg"))
    }
}
