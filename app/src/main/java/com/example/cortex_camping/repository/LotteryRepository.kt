/**
 * Repository responsible for managing lottery data in Firebase.
 * Handles creating and updating lotteries, entering and leaving lotteries,
 * managing waitlists, and converting lottery to and back for Firebase to use.
 */
package com.example.cortex_camping.repository

import androidx.compose.runtime.mutableStateListOf
import com.example.cortex_camping.model.Lottery
import com.example.cortex_camping.model.LotteryEntry
import com.example.cortex_camping.model.LotteryStatus
import com.example.cortex_camping.model.Reservation
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Transaction
import com.google.firebase.firestore.firestore
import java.time.LocalDate
import java.time.LocalDateTime

class LotteryRepository {

    private val db = Firebase.firestore
    private val lotteriesRef = db.collection("lotteries")

    private val campsitesRef = db.collection("campsite")

    private val reservationsRef = db.collection("reservations")

    private val nightsRef = db.collection("bookedNights")

    private val _lotteries = mutableStateListOf<Lottery>()

    val lotteries: List<Lottery>
        get() = _lotteries

    // waits for changes to Firestore
    init {
        lotteriesRef.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            _lotteries.clear()
            snapshot?.documents?.forEach { document ->
                val lottery = document.data?.let {
                    documentToLottery(document.id, it)
                }
                // if space in lottery, add
                if (lottery != null) {
                    _lotteries.add(lottery)
                }
            }
        }
    }

    // saves a new lottery or updates an existing one
    fun saveLottery(lottery: Lottery, onResult: (String?) -> Unit = {}) {
        // check if an id exists already, if not generate one
        val document = if (lottery.id.isBlank()) {
            lotteriesRef.document()
        } else {
            lotteriesRef.document(lottery.id)
        }

        val updatedLottery = lottery.copy(id = document.id)

        document.set(lotteryToMap(updatedLottery))
            // check if successful or not
            .addOnSuccessListener {
                onResult(null)
            }
            .addOnFailureListener {
                onResult(it.message ?: "Could not save lottery.")
            }
    }

    // adds the entrant on the screen to the lotto
    fun enterLottery(
        lotteryId: String,
        entry: LotteryEntry,
        onResult: (String?) -> Unit = {}
    ) {
        updateLottery(lotteryId, onResult) { lottery ->
            // check if the lotto is currently ongoing
            check(lottery.status == LotteryStatus.ONGOING) {
                "This lottery is already over."
            }
            // check if lotto can accept new entries
            check(isOpen(lottery)) {
                "This lottery is currently not accepting entries."
            }
            // check if user is in either lotto or waitlist
            check(lottery.entryFor(entry.userId) == null &&
                    !lottery.isWaitlisted(entry.userId)) {
                "You have already in this lottery."
            }
            // check if lotto is full
            check(!lottery.isFull) {
                "This lottery is full. Join the waitlist instead."
            }
            lottery.copy(entries = lottery.entries + entry)
        }
    }

    // removes an entrant in the lotto, put the first in waitlist into the lotto
    fun leaveLottery(
        lotteryId: String,
        userId: String,
        onResult: (String?) -> Unit = {}
    ) {
        updateLottery(lotteryId, onResult) { lottery ->
            check(lottery.status == LotteryStatus.ONGOING) {
                "This lottery is already over"
            }
            val remainingEntries =
                lottery.entries.filterNot { it.userId == userId }
            check(remainingEntries.size < lottery.entries.size) {
                "You are yet to be entered into this lottery."
            }
            // first person waiting gets into the lottery
            val firstWaiting = lottery.waitlist.firstOrNull()

            lottery.copy(
                entries = if (firstWaiting != null) {
                    remainingEntries + firstWaiting
                } else {
                    remainingEntries
                },
                waitlist = if (firstWaiting != null) {
                    lottery.waitlist.drop(1)
                } else {
                    lottery.waitlist
                }
            )
        }
    }

    // entrant added to the waitlist
    fun joinWaitlist(
        lotteryId: String,
        entry: LotteryEntry,
        onResult: (String?) -> Unit = {}
    ) {
        updateLottery(lotteryId, onResult) { lottery ->
            check(lottery.status == LotteryStatus.ONGOING) {
                "This lottery is already over."
            }

            check(isOpen(lottery)) {
                "This lottery is not currently accepting entries."
            }

            check(lottery.isFull) {
                "There is still room in this lottery. Enter it directly."
            }

            check(lottery.hasWaitlist) {
                "The waitlist is not available."
            }

            check(lottery.entryFor(entry.userId) == null &&
                    !lottery.isWaitlisted(entry.userId)) {
                "You have already joined this waitlist."
            }

            check(lottery.waitlist.size < lottery.waitlistCapacity) {
                "The waitlist is full."
            }

            lottery.copy(waitlist = lottery.waitlist + entry)
        }
    }

    // remove an entrant from the waitlist
    fun leaveWaitlist(
        lotteryId: String,
        userId: String,
        onResult: (String?) -> Unit = {}
    ) {
        updateLottery(lotteryId, onResult) { lottery ->
            val remaining =
                lottery.waitlist.filterNot { it.userId == userId }
            check(remaining.size < lottery.waitlist.size) {
                "You are not on this waitlist."
            }
            lottery.copy(waitlist = remaining)
        }
    }

    // a winner books their stay: picks one of the lottery's campsites and their dates.
    fun claimWin(
        lotteryId: String,
        reservation: Reservation,
        onResult: (String?) -> Unit = {}
    ) {
        if (lotteryId.isBlank()) {
            onResult("Invalid lottery ID.")
            return
        }
        val lotteryDoc = lotteriesRef.document(lotteryId)
        val reservationDoc = reservationsRef.document(reservation.bookingId)

        db.runTransaction { transaction ->
            val lottery = transaction.readLottery(lotteryDoc)

            check(lottery.hasOpenOffer(reservation.userId)) {
                "You don't have an open offer for this lottery."
            }
            check(reservation.campsiteId in lottery.campsiteIds) {
                "That campsite isn't part of this lottery."
            }
            check(reservation.nights in 1..lottery.nights) {
                "Pick a stay of up to ${lottery.nights} nights."
            }
            check(!reservation.checkIn.isBefore(lottery.stayFrom) &&
                    !reservation.checkOut.isAfter(lottery.stayUntil)) {
                "Those dates are outside this lottery's stay window."
            }

            // the campsite's own availability: its open window and the nights the owner blocked off
            val campsiteData = transaction.get(campsitesRef.document(reservation.campsiteId)).data
                ?: throw IllegalStateException("That campsite no longer exists.")
            val availability = campsiteData["availability"] as? Map<*, *>
            fun dateOf(key: String) = (availability?.get(key) as? String)
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            val availableFrom = dateOf("availableFrom")
            val availableUntil = dateOf("availableUntil")
            val blocked = (availability?.get("unavailableDates") as? List<*>)
                ?.filterIsInstance<String>()?.toSet() ?: emptySet()

            check((availableFrom == null || !reservation.checkIn.isBefore(availableFrom)) &&
                    (availableUntil == null || !reservation.checkOut.isAfter(availableUntil))) {
                "The campsite isn't open for those dates."            }
            transaction.set(reservationDoc, ReservationRepository.toMap(reservation))
            transaction.update(
                lotteryDoc,
                mapOf("claimedIds" to FieldValue.arrayUnion(reservation.userId))
            )
            null
        }
            .addOnSuccessListener { onResult(null) }
            .addOnFailureListener { onResult(it.message ?: "Something went wrong.") }
    }

    // a winner gives their win up, which also releases every campsite in the lottery
    fun declineWin(
        lotteryId: String,
        userId: String,
        onResult: (String?) -> Unit = {}
    ) {
        if (lotteryId.isBlank()) {
            onResult("Invalid lottery ID.")
            return
        }
        val lotteryDoc = lotteriesRef.document(lotteryId)

        db.runTransaction { transaction ->
            val lottery = transaction.readLottery(lotteryDoc)
            check(lottery.hasOpenOffer(userId)) {
                "There's no open offer to decline."
            }
            transaction.update(
                lotteryDoc,
                mapOf("declinedIds" to FieldValue.arrayUnion(userId))
            )
            null
        }
            .addOnSuccessListener { onResult(null) }
            .addOnFailureListener { onResult(it.message ?: "Something went wrong.") }
    }

    private fun Transaction.readLottery(document: DocumentReference): Lottery {
        val snapshot = get(document)
        val data = snapshot.data
            ?: throw IllegalStateException("Lottery not found.")
        return documentToLottery(snapshot.id, data)
            ?: throw IllegalStateException("Could not read lottery.")
    }

    // check if entries are open
    private fun isOpen(lottery: Lottery): Boolean {
        val now = LocalDateTime.now()

        return !now.isBefore(lottery.opensAt) &&
                !now.isAfter(lottery.closesAt)
    }

    private fun updateLottery(
        lotteryId: String,
        onResult: (String?) -> Unit,
        update: (Lottery) -> Lottery
    ) {
        if (lotteryId.isBlank()) {
            onResult("Invalid lottery ID.")
            return
        }

        val document = lotteriesRef.document(lotteryId)

        db.runTransaction { transaction ->

            val snapshot = transaction.get(document)

            val data = snapshot.data
                ?: throw IllegalStateException("Lottery not found.")

            val current = documentToLottery(snapshot.id, data)
                ?: throw IllegalStateException("Could not read lottery.")

            val updated = update(current)

            transaction.update(
                document,
                mapOf(
                    "entries" to updated.entries.map { entryToMap(it) },
                    "waitlist" to updated.waitlist.map { entryToMap(it) }
                )
            )
            null
        }
            .addOnSuccessListener {
                onResult(null)
            }
            .addOnFailureListener {
                onResult(it.message ?: "Something went wrong.")
            }
    }

    // converts our lotto to a firebase format
    private fun lotteryToMap(lottery: Lottery): Map<String, Any> {
        return mapOf(
            "id" to lottery.id,
            "ownerId" to lottery.ownerId,
            "name" to lottery.name,
            "campsiteIds" to lottery.campsiteIds,
            "status" to lottery.status.name,
            "capacity" to lottery.capacity,
            "winners" to lottery.winners,
            "waitlistCapacity" to lottery.waitlistCapacity,
            "entries" to lottery.entries.map { entryToMap(it) },
            "waitlist" to lottery.waitlist.map { entryToMap(it) },
            "winnerIds" to lottery.winnerIds,
            "opensAt" to lottery.opensAt.toString(),
            "closesAt" to lottery.closesAt.toString(),
            "drawAt" to lottery.drawAt.toString(),
            "stayFrom" to lottery.stayFrom.toString(),
            "stayUntil" to lottery.stayUntil.toString(),
            "nights" to lottery.nights,
            "claimedIds" to lottery.claimedIds,
            "declinedIds" to lottery.declinedIds
        )
    }

    private fun entryToMap(entry: LotteryEntry): Map<String, String> {
        return mapOf(
            "userId" to entry.userId,
            "campsiteId" to entry.campsiteId
        )
    }

    // convert firebase data back into lotto
    private fun documentToLottery(
        id: String,
        data: Map<String, Any?>
    ): Lottery? {
        return try {
            Lottery(
                id = id,
                ownerId = data["ownerId"] as? String ?: "",
                name = data["name"] as? String ?: "",
                campsiteIds = (data["campsiteIds"] as? List<*>)
                    ?.filterIsInstance<String>() ?: emptyList(),
                status = (data["status"] as? String)
                    ?.let { LotteryStatus.valueOf(it) }
                    ?: LotteryStatus.ONGOING,
                capacity = (data["capacity"] as? Number)?.toInt() ?: 50,
                winners = (data["winners"] as? Number)?.toInt() ?: 3,
                waitlistCapacity =
                    (data["waitlistCapacity"] as? Number)?.toInt() ?: 0,
                entries = readEntries(data["entries"]),
                waitlist = readEntries(data["waitlist"]),
                winnerIds = (data["winnerIds"] as? List<*>)
                    ?.filterIsInstance<String>() ?: emptyList(),
                opensAt = LocalDateTime.parse(
                    data["opensAt"] as? String
                        ?: LocalDateTime.now().toString()
                ),
                closesAt = LocalDateTime.parse(
                    data["closesAt"] as? String
                        ?: LocalDateTime.now().plusDays(1).toString()
                ),
                drawAt = LocalDateTime.parse(
                    data["drawAt"] as? String
                        ?: LocalDateTime.now().plusDays(2).toString()
                ),
                stayFrom = LocalDate.parse(
                    data["stayFrom"] as? String
                        ?: LocalDate.now().plusMonths(1).toString()
                ),
                stayUntil = LocalDate.parse(
                    data["stayUntil"] as? String
                        ?: LocalDate.now().plusMonths(2).toString()
                ),
                nights = (data["nights"] as? Number)?.toInt() ?: 2,
                claimedIds = (data["claimedIds"] as? List<*>)
                    ?.filterIsInstance<String>() ?: emptyList(),
                declinedIds = (data["declinedIds"] as? List<*>)
                    ?.filterIsInstance<String>() ?: emptyList()
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun readEntries(value: Any?): List<LotteryEntry> {
        return (value as? List<*>)
            ?.mapNotNull { item ->
                val entry = item as? Map<*, *> ?: return@mapNotNull null

                LotteryEntry(
                    userId = entry["userId"] as? String ?: "",
                    campsiteId = entry["campsiteId"] as? String ?: ""
                )
            } ?: emptyList()
    }
}
