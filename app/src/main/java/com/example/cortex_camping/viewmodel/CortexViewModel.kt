package com.example.cortex_camping.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.example.cortex_camping.model.AppNotification
import com.example.cortex_camping.model.Campsite
import com.example.cortex_camping.model.CampsiteOwner
import com.example.cortex_camping.model.Entrant
import com.example.cortex_camping.model.Lottery
import com.example.cortex_camping.model.Message
import com.example.cortex_camping.model.Reservation
import com.example.cortex_camping.model.ReservationStatus
import com.example.cortex_camping.ui.screens.admin.CommunicationLog
import com.example.cortex_camping.ui.screens.admin.LogType
import com.example.cortex_camping.ui.screens.admin.ManagedUser
import com.example.cortex_camping.ui.screens.owner.GuestReservation
import com.example.cortex_camping.util.DeviceUtils
import com.example.cortex_camping.util.ImageUtils
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import com.example.cortex_camping.model.LotteryEntry
import com.example.cortex_camping.model.LotteryStatus
import com.example.cortex_camping.repository.LotteryRepository
import com.example.cortex_camping.repository.MessageRepository
import com.example.cortex_camping.repository.CampsiteRepository
import java.util.UUID
import com.example.cortex_camping.repository.AdminModerationRepository

enum class Role(val label: String) { User("Camper"), Owner("Campsite owner"), Admin("Admin") }

class CortexViewModel(application: Application) : AndroidViewModel(application) {

    val deviceId: String = DeviceUtils.getDeviceId(application)

    private val campsiteRepository = CampsiteRepository()
    private val moderationRepository = AdminModerationRepository()

    override fun onCleared() {
        moderationRepository.close()
        super.onCleared()
    }

    val isLoaded: Boolean get() = true

    var role by mutableStateOf(Role.User)

    // Owner/profile startup routing is still handled separately.
    fun startingRole(): Role = Role.User

    // Current user

    // TODO: look up deviceId in EntrantRepository
    val entrant: Entrant? get() = null

    // TODO: look up deviceId in CampsiteOwnerRepository
    val owner: CampsiteOwner? get() = null

    val isAdmin: Boolean get() = role == Role.Admin
    val adminError: String? get() = moderationRepository.error

    val isRegistered: Boolean get() = entrant != null

    // TODO: look up the name in EntrantRepository
    fun entrantName(id: String): String = "Camper"

    // Moderation profiles provide owner names on the admin browse screens.
    fun ownerName(id: String): String = managedUsers.firstOrNull { it.isOwner && it.id == id }?.name ?: "Campsite owner"

    // Campsites

    val campsites: List<Campsite> get() = campsiteRepository.campsites

    fun campsite(id: String): Campsite? = campsites.firstOrNull { it.campsiteId == id }

    // TODO: add the nights already booked by other reservations at this campsite
    fun takenDates(campsite: Campsite): Set<LocalDate> = campsite.availability.unavailableDates

    // Profile

    // TODO: save a new Entrant with EntrantRepository.saveOrUpdateEntrant
    fun register(name: String, email: String, phone: String) {}

    // TODO: save the changes to the current entrant
    fun updateProfile(name: String, email: String, phone: String) {}

    // TODO: save the entrant's notification settings
    fun updatePrefs(updated: Entrant) {}

    // TODO: EntrantRepository.deleteEntrant
    fun deleteProfile() {}

    // Reservations (entrant)

    // TODO: the entrant's active reservations that aren't cancelled
    val upcomingReservations: List<Reservation> get() = emptyList()

    // TODO: the entrant's past reservations
    val pastReservations: List<Reservation> get() = emptyList()

    // TODO: lotteries the entrant won but hasn't picked dates for yet
    val bookingOffers: List<Lottery> get() = emptyList()

    fun offerCampsite(lottery: Lottery): Campsite? = lottery.entryFor(deviceId)?.campsiteId?.let(::campsite)

    // TODO: create the Reservation for this lottery win and let the owner know
    fun chooseDates(lottery: Lottery, checkIn: LocalDate, checkOut: LocalDate) {}

    // TODO: give the lottery win up
    fun declineOffer(lottery: Lottery) {}

    // TODO: EntrantRepository.cancelReservation, then let the owner know
    fun cancelReservation(reservation: Reservation) {}

    // TODO: save the rating on the reservation and update the campsite's average
    fun rate(reservation: Reservation, stars: Int) {}

    // Lotteries (entrant)

    // lotteries the entrant has entered
    val myLotteries: List<Lottery>
        get() = lotteryRepository.lotteries.filter {
        it.entryFor(deviceId) != null
    }

    private val lotteryRepository = LotteryRepository()
    private val messageRepository = MessageRepository()

    // TODO: lotteries that include this campsite
    fun lotteriesFor(campsiteId: String): List<Lottery> =
        lotteryRepository.lotteries.filter {
            campsiteId in it.campsiteIds
        }

    // add a lotto entry
    fun enterLottery(
        lottery: Lottery,
        campsiteId: String,
        onResult: (String?) -> Unit = {}
    ) {
        lotteryRepository.enterLottery(
            lottery.id,
            LotteryEntry(deviceId, campsiteId),
            onResult
        )
    }

    // leave a lotto
    fun leaveLottery(
        lottery: Lottery,
        onResult: (String?) -> Unit = {}
    ) {
        lotteryRepository.leaveLottery(
            lottery.id,
            deviceId,
            onResult
        )
    }

    // joins a waitlist if there is room
    fun joinWaitlist(
        lottery: Lottery,
        campsiteId: String,
        onResult: (String?) -> Unit = {}
    ) {
        lotteryRepository.joinWaitlist(
            lottery.id,
            LotteryEntry(deviceId, campsiteId),
            onResult
        )
    }

    // removes an entrant from the waitlist
    fun leaveWaitlist(
        lottery: Lottery,
        onResult: (String?) -> Unit = {}
    ) {
        lotteryRepository.leaveWaitlist(
            lottery.id,
            deviceId,
            onResult
        )
    }

    // Owner

    // TODO: create a CampsiteOwner for this device if there isn't one yet
    fun ensureOwner() {}

    val myCampsites: List<Campsite>
        get() = campsiteRepository.campsites.filter { it.ownerId == deviceId }

    // lotteries where ownerId == deviceId
    val ownerLotteries: List<Lottery>
        get() = lotteryRepository.lotteries.filter {
            it.ownerId == deviceId
        }

    fun hasOngoingLottery(campsite: Campsite): Boolean =
        lotteriesFor(campsite.campsiteId).any { it.status == LotteryStatus.ONGOING }

    // TODO: every entrant's reservations at the owner's campsites, with the guest's name
    val ownerReservations: List<GuestReservation> get() = emptyList()

    /**
     * Shrinks a newly picked photo into the campsite, then writes it. If the photo can't be
     * processed, [onError] runs and nothing is saved: a device-local Uri is useless in Firestore.
     */
    fun saveCampsite(campsite: Campsite, onSaved: () -> Unit = {}, onError: () -> Unit = {}) {
        val campsiteId = campsite.campsiteId.ifBlank { UUID.randomUUID().toString() }
        val ownerId = campsite.ownerId.ifBlank { deviceId }
        val base = campsite.copy(campsiteId = campsiteId, ownerId = ownerId)
        val image = base.imageUrl

        if (image == null || !ImageUtils.isLocalUri(image)) {
            campsiteRepository.addCampsite(base)
            onSaved()
            return
        }
        viewModelScope.launch {
            runCatching { ImageUtils.encodeAsDataUri(getApplication(), image) }
                .onSuccess { encoded ->
                    campsiteRepository.addCampsite(base.copy(imageUrl = encoded))
                    onSaved()
                }
                .onFailure { error ->
                    Log.e("CortexViewModel", "Couldn't process image for campsite $campsiteId", error)
                    // an older save may have stored a local Uri; don't let it block other edits
                    if (image == campsite(campsiteId)?.imageUrl) {
                        campsiteRepository.addCampsite(base)
                        onSaved()
                    } else {
                        onError()
                    }
                }
        }
    }

    // TODO: save the new status on the entrant's reservation and notify them
    fun setReservationStatus(item: GuestReservation, status: ReservationStatus) {}

    // create the lottery (blank id) or save the changes
    fun saveLottery(lottery: Lottery, onResult: (String?) -> Unit = {}) {
        lotteryRepository.saveLottery(lottery.copy(ownerId = lottery.ownerId.ifBlank { deviceId }), onResult)
    }

    // TODO: send a notification to everyone on the waitlist
    fun notifyWaitlist(lottery: Lottery) {}

    // TODO: pick the winners, finalize the lottery and notify every entrant
    fun runDraw(lottery: Lottery) {}

    fun bookedDates(campsite: Campsite): Set<LocalDate> = takenDates(campsite)

    // Notifications

    // TODO: notifications sent to this device as an entrant
    val myNotifications: List<AppNotification> get() = emptyList()

    // TODO: notifications sent to this device as an owner (forOwner = true)
    val ownerReceived: List<AppNotification> get() = emptyList()

    // TODO: notifications this owner sent
    val ownerSent: List<AppNotification> get() = emptyList()

    val unreadCount: Int get() = myNotifications.count { !it.read }
    val ownerUnreadCount: Int get() = ownerReceived.count { !it.read }

    // TODO: mark these notifications as read
    fun markRead(notifications: List<AppNotification>) {}

    fun notificationsCsv(notifications: List<AppNotification>): String = buildString {
        appendLine("title,detail,link,sent,read")
        notifications.forEach { n ->
            appendLine(listOf(n.title, n.body, n.link, n.sentAt.toString(), n.read.toString())
                .joinToString(",") { "\"" + it.replace("\"", "\"\"") + "\"" })
        }
    }

    // Chat

    // TODO: messagesz between this entrant and the campsite's owner, oldest first
    fun conversation(campsiteId: String, entrantId: String): List<Message> =
        messageRepository.conversation(campsiteId, entrantId)

    // TODO: save the message and notify the other person
    fun sendMessage(campsiteId: String, entrantId: String, text: String) {
        messageRepository.sendMessage(
            campsiteId = campsiteId,
            entrantId = entrantId,
            senderId = deviceId,
            text = text
        )
    }

    // Admin

    // Profiles displayed by the admin user browser.
    val managedUsers: List<ManagedUser> get() = moderationRepository.users

    // Delete the selected role's profile, preserving independent records.
    fun deleteUser(user: ManagedUser, onResult: (String?) -> Unit) {
        moderationRepository.deleteUser(user, onResult)
    }

    // Completion callbacks report the server result to the confirmation flow.
    fun deleteCampsite(campsite: Campsite, onResult: (String?) -> Unit) {
        moderationRepository.deleteCampsite(campsite.campsiteId, onResult)
    }

    val campsitesWithImages: List<Campsite> get() = campsites.filter { !it.imageUrl.isNullOrBlank() }

    // Remove only the reviewed image field, without overwriting campsite edits.
    fun deleteImage(campsite: Campsite, onResult: (String?) -> Unit) {
        moderationRepository.deleteImage(campsite.campsiteId, campsite.imageUrl, onResult)
    }

    // TODO: include notifications once notification storage is implemented
    // TODO: improve formatting of log info (e.g. convert timestamp, use name instead of id)
    val communicationLogs: List<CommunicationLog>
        get() = messageRepository.messages
            .sortedByDescending { it.sentAt }
            .map { message ->
                CommunicationLog(
                    id = "message-${message.id.ifBlank { "${message.campsiteId}-${message.entrantId}-${message.sentAt}" }}",
                    type = LogType.Message,
                    subject = "Campsite ${message.campsiteId}",
                    route = "Room ${message.campsiteId}/${message.entrantId}",
                    meta = "Sender ${message.senderId} · Entrant ${message.entrantId} · ${message.sentAt}",
                    message = message.text
                )
            }
}
