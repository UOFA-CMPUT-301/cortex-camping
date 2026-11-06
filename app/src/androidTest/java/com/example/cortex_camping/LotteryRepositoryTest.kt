/**
 *
 * Tests LotteryRepository to confirm Firebase works for
 * US 01.01.06, US 01.02,01, US 01.02,02, focuses on
 * the joining and leaving of lottery,
 *
 *
 */

package com.example.cortex_camping

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.cortex_camping.model.Lottery
import com.example.cortex_camping.model.LotteryEntry
import com.example.cortex_camping.model.LotteryStatus
import com.example.cortex_camping.repository.LotteryRepository
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Firebase
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.firestore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class LotteryRepositoryTest {
    private val TEST_OWNER = "test-owner-cortex"
    private val TEST_CAMPSITE = "test-campsite"
    private val TIMEOUT_SECONDS = 20L

    private val lotteriesRef
        get() = Firebase.firestore.collection("lotteries")

    private lateinit var repo: LotteryRepository
    private val created = mutableListOf<String>()

    // names of users for testing
    private val alice = "test-user-alice"
    private val bob = "test-user-bob"
    private val carol = "test-user-carol"
    private val dave = "test-user-dave"

    @Before
    fun setUp() {
        repo = LotteryRepository()
    }

    // make sure that no lottos are left after testing
    @After
    fun deleteTestLotteries() {
        created.forEach { id ->
            Tasks.await(
                lotteriesRef.document(id).delete(),
                TIMEOUT_SECONDS,
                TimeUnit.SECONDS
            )

            val snapshot = Tasks.await(
                lotteriesRef.document(id).get(Source.SERVER),
                TIMEOUT_SECONDS,
                TimeUnit.SECONDS
            )

            assertFalse("Test lottery $id was not deleted", snapshot.exists())
        }
        created.clear()
    }

    // enter lotto tests
    @Test
    fun enterLottery_addsEntrant() {
        val lottery = newLottery()

        assertNull(enter(lottery, alice))
        assertEquals(listOf(alice), entries(lottery))
        assertEquals(emptyList<String>(), waitlist(lottery))
    }

    @Test
    fun enterLottery_keepsJoinOrder() {
        val lottery = newLottery()

        assertNull(enter(lottery, alice))
        assertNull(enter(lottery, bob))
        assertEquals(listOf(alice, bob), entries(lottery))
    }

    @Test
    fun enterLottery_rejectsDuplicate() {
        val lottery = newLottery()

        assertNull(enter(lottery, alice))
        assertNotNull("Entering twice should fail", enter(lottery, alice))
        assertEquals(listOf(alice), entries(lottery))
    }

    @Test
    fun enterLottery_rejectsWhenFull() {
        val lottery = newLottery(capacity = 2)
        fill(lottery)
        assertNotNull("Entering a full lottery should fail", enter(lottery, carol))
        assertEquals(listOf(alice, bob), entries(lottery))
    }

    @Test
    fun enterLottery_rejectsAfterRegistrationClosed() {
        val lottery = newLottery(
            opensAt = LocalDateTime.now().minusDays(1),
            closesAt = LocalDateTime.now().minusMinutes(1)
        )
        assertNotNull("Entering after closesAt should fail", enter(lottery, alice))
        assertEquals(emptyList<String>(), entries(lottery))
    }

    @Test
    fun enterLottery_rejectsBeforeRegistrationOpens() {
        val lottery = newLottery(
            opensAt = LocalDateTime.now().plusHours(1),
            closesAt = LocalDateTime.now().plusDays(1)
        )
        assertNotNull("Entering before opensAt should fail", enter(lottery, alice))
        assertEquals(emptyList<String>(), entries(lottery))
    }

    @Test
    fun enterLottery_rejectsFinalizedLottery() {
        val lottery = newLottery(status = LotteryStatus.FINALIZED)
        assertNotNull("Entering a finalized lottery should fail", enter(lottery, alice))
        assertEquals(emptyList<String>(), entries(lottery))
    }

    @Test
    fun enterLottery_rejectsUnknownLottery() {
        val error = call {
            repo.enterLottery(
                "test-lottery-does-not-exist",
                LotteryEntry(alice, TEST_CAMPSITE),
                it
            )
        }
        assertNotNull("Entering a lottery that doesn't exist should fail", error)
    }

    // leave lotto tests

    @Test
    fun leaveLottery_removesEntrant() {
        val lottery = newLottery()
        assertNull(enter(lottery, alice))
        assertNull(enter(lottery, bob))
        assertNull(leave(lottery, alice))
        assertEquals(listOf(bob), entries(lottery))
    }

    @Test
    fun leaveLottery_promotesFirstPersonOnWaitlist() {
        val lottery = newLottery(capacity = 2, waitlistCapacity = 2)
        fill(lottery)
        assertNull(joinWaitlist(lottery, carol))
        assertNull(joinWaitlist(lottery, dave))
        assertNull(leave(lottery, alice))
        assertEquals(
            "Carol was first in line so she takes Alice's spot",
            listOf(bob, carol),
            entries(lottery)
        )
        assertEquals(listOf(dave), waitlist(lottery))
    }

    @Test
    fun leaveLottery_rejectsEntrantWhoNeverEntered() {
        val lottery = newLottery()

        assertNull(enter(lottery, alice))
        assertNotNull("Leaving without entering should fail", leave(lottery, carol))
        assertEquals(listOf(alice), entries(lottery))
    }

    // joinWaitlist tests

    @Test
    fun joinWaitlist_addsEntrantWhenLotteryIsFull() {
        val lottery = newLottery(capacity = 2)
        fill(lottery)
        assertNull(joinWaitlist(lottery, carol))
        assertEquals(listOf(carol), waitlist(lottery))
        assertEquals(listOf(alice, bob), entries(lottery))
    }

    @Test
    fun joinWaitlist_keepsJoinOrder() {
        val lottery = newLottery(capacity = 2, waitlistCapacity = 2)
        fill(lottery)
        assertNull(joinWaitlist(lottery, carol))
        assertNull(joinWaitlist(lottery, dave))
        assertEquals(listOf(carol, dave), waitlist(lottery))
    }

    @Test
    fun joinWaitlist_rejectsWhenLotteryStillHasRoom() {
        val lottery = newLottery(capacity = 2)
        assertNull(enter(lottery, alice))
        assertNotNull("Waitlist is only for a full lottery", joinWaitlist(lottery, carol))
        assertEquals(emptyList<String>(), waitlist(lottery))
    }

    @Test
    fun joinWaitlist_rejectsDuplicate() {
        val lottery = newLottery(capacity = 2)
        fill(lottery)
        assertNull(joinWaitlist(lottery, carol))
        assertNotNull("Joining the waitlist twice should fail", joinWaitlist(lottery, carol))
        assertEquals(listOf(carol), waitlist(lottery))
    }

    @Test
    fun joinWaitlist_rejectsSomeoneAlreadyInTheDraw() {
        val lottery = newLottery(capacity = 2)
        fill(lottery)
        assertNotNull("Alice is already in the draw", joinWaitlist(lottery, alice))
        assertEquals(emptyList<String>(), waitlist(lottery))
    }

    @Test
    fun joinWaitlist_rejectsWhenWaitlistIsFull() {
        val lottery = newLottery(capacity = 2, waitlistCapacity = 1)
        fill(lottery)
        assertNull(joinWaitlist(lottery, carol))
        assertNotNull("The waitlist only holds one person", joinWaitlist(lottery, dave))
        assertEquals(listOf(carol), waitlist(lottery))
    }

    @Test
    fun joinWaitlist_rejectsWhenWaitlistIsTurnedOff() {
        val lottery = newLottery(capacity = 2, waitlistCapacity = 0)
        fill(lottery)
        assertNotNull("waitlistCapacity 0 means no waitlist", joinWaitlist(lottery, carol))
        assertEquals(emptyList<String>(), waitlist(lottery))
    }

    // leaveWaitlistTests

    @Test
    fun leaveWaitlist_removesEntrant() {
        val lottery = newLottery(capacity = 2, waitlistCapacity = 2)
        fill(lottery)
        assertNull(joinWaitlist(lottery, carol))
        assertNull(joinWaitlist(lottery, dave))
        assertNull(leaveWaitlist(lottery, carol))
        assertEquals(listOf(dave), waitlist(lottery))
        assertEquals(listOf(alice, bob), entries(lottery))
    }

    @Test
    fun leaveWaitlist_rejectsSomeoneNotOnTheWaitlist() {
        val lottery = newLottery(capacity = 2)
        fill(lottery)
        assertNotNull("Carol was never on the waitlist", leaveWaitlist(lottery, carol))
    }

    @Test
    fun leaveWaitlist_doesNotRemoveSomeoneFromTheDraw() {
        val lottery = newLottery(capacity = 2)
        fill(lottery)
        assertNotNull("Alice is in the draw, not on the waitlist", leaveWaitlist(lottery, alice))
        assertEquals(listOf(alice, bob), entries(lottery))
    }

    // test functions to simplify the functions we call

    private fun newLottery(
        capacity: Int = 2,
        waitlistCapacity: Int = 2,
        status: LotteryStatus = LotteryStatus.ONGOING,
        opensAt: LocalDateTime = LocalDateTime.now().minusHours(1),
        closesAt: LocalDateTime = LocalDateTime.now().plusDays(1)
    ): Lottery {

        val lottery = Lottery(
            id = "test-lottery-${UUID.randomUUID()}",
            ownerId = TEST_OWNER,
            name = "TEST LOTTERY (safe to delete)",
            campsiteIds = listOf(TEST_CAMPSITE),
            status = status,
            capacity = capacity,
            winners = 1,
            waitlistCapacity = waitlistCapacity,
            opensAt = opensAt,
            closesAt = closesAt,
            drawAt = closesAt.plusDays(1)
        )

        created += lottery.id

        assertNull("Could not save the test lottery", call {
            repo.saveLottery(lottery, it)
        })

        return lottery
    }

    private fun fill(lottery: Lottery) {
        assertNull(enter(lottery, alice))
        assertNull(enter(lottery, bob))
    }

    private fun enter(lottery: Lottery, user: String) =
        call {
            repo.enterLottery(
                lottery.id,
                LotteryEntry(user, TEST_CAMPSITE),
                it
            )
        }

    private fun leave(lottery: Lottery, user: String) =
        call {
            repo.leaveLottery(lottery.id, user, it)
        }

    private fun joinWaitlist(lottery: Lottery, user: String) =
        call {
            repo.joinWaitlist(
                lottery.id,
                LotteryEntry(user, TEST_CAMPSITE),
                it
            )
        }

    private fun leaveWaitlist(lottery: Lottery, user: String) =
        call {
            repo.leaveWaitlist(lottery.id, user, it)
        }

    // Waits for the repository callback to finish
    private fun call(block: ((String?) -> Unit) -> Unit): String? {
        val latch = CountDownLatch(1)
        var error: String? = null

        block {
            error = it
            latch.countDown()
        }

        assertTrue(
            "Timed out waiting for Firestore",
            latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        )

        return error
    }

    private fun entries(lottery: Lottery) =
        userIds(lottery, "entries")

    private fun waitlist(lottery: Lottery) =
        userIds(lottery, "waitlist")

    // Reads the saved document directly from Firebase
    private fun userIds(lottery: Lottery, field: String): List<String> {
        val snapshot = Tasks.await(
            lotteriesRef.document(lottery.id).get(Source.SERVER),
            TIMEOUT_SECONDS,
            TimeUnit.SECONDS
        )

        return (snapshot.get(field) as? List<*>)
            ?.mapNotNull {
                (it as? Map<*, *>)?.get("userId") as? String
            }
            .orEmpty()
    }
}