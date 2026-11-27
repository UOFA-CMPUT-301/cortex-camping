package com.example.cortex_camping

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.cortex_camping.repository.NotificationRepository
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationTests {
    // Initialization
    private val TEST_OWNER = "test-owner-cortex"
    private lateinit var repo: NotificationRepository

    // names of users for testing (taken from Lottery Repository Test, mainly for consistency's sake)
    private val alice = "test-user-alice"
    private val bob = "test-user-bob"
    private val carol = "test-user-carol"
    private val dave = "test-user-dave"
}