package com.example.cortex_camping.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.example.cortex_camping.model.AppNotification
import com.example.cortex_camping.model.NotificationKind
import com.example.cortex_camping.model.NotificationTarget
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate
import com.example.cortex_camping.model.LotteryEntry
import com.example.cortex_camping.model.LotteryStatus
import com.example.cortex_camping.repository.LotteryRepository
import com.example.cortex_camping.repository.MessageRepository
import com.example.cortex_camping.repository.CampsiteImageRepository
import com.example.cortex_camping.repository.CampsiteRepository
import com.example.cortex_camping.repository.ReservationRepository
import java.util.UUID
import com.example.cortex_camping.repository.AdminModerationRepository
import com.example.cortex_camping.repository.EntrantRepository
import com.example.cortex_camping.repository.NotificationRepository
import com.example.cortex_camping.model.ReservationNotifications
import com.example.cortex_camping.model.CampsiteRating
import com.example.cortex_camping.model.past
import com.example.cortex_camping.model.upcoming

enum class Role(val label: String) { User("Camper"), Owner("Campsite owner"), Admin("Admin") }

class CortexViewModel(application: Application) : AndroidViewModel(application) {

    val deviceId: String = DeviceUtils.getDeviceId(application)


    private val entrantRepository = EntrantRepository()
    private val campsiteRepository = CampsiteRepository()
    private val imageRepository = CampsiteImageRepository()
    private val moderationRepository = AdminModerationRepository()

    override fun onCleared() {
        moderationRepository.close()
        super.onCleared()
    }

    private val reservationRepository = ReservationRepository()
    private val notificationRepository = NotificationRepository()

    // TODO: return false until the entrant, owner and admin repositories have loaded,
    //  otherwise the app picks its start screen before it knows who the user is
    val isLoaded: Boolean get() = true

    var role by mutableStateOf(Role.User)

    // Owner/profile startup routing is still handled separately.
    fun startingRole(): Role = Role.User

    // Current user

    // looks up deviceId in the entrant database
    val entrant: Entrant? get() = entrantRepository.entrants.firstOrNull { it.id == deviceId }

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

    // nights the owner don't allow plus nights already booked are taken
    fun takenDates(campsite: Campsite): Set<LocalDate> =
        campsite.availability.unavailableDates + reservationRepository.bookedDates(campsite.campsiteId)

    // check-in dates a winner is allowed to book after winning
    fun openCheckIns(lottery: Lottery, campsite: Campsite): List<LocalDate> =
        lottery.availableCheckIns(
            takenDates(campsite),
            campsite.availability.availableFrom,
            campsite.availability.availableUntil
        )

    // Profile

    // saves entrant data to the database, using deviceID as their id
    fun register(name: String, email: String, phone: String) {
        val newEntrant = Entrant(
            id = deviceId,
            name = name,
            email = email,
            phoneNumber = phone,
        )
        entrantRepository.saveOrUpdateEntrant(deviceId, newEntrant)
    }
    // updates user profile in firebase
    fun updateProfile(name: String, email: String, phone: String) {
        val current = entrant ?: return
        val updated = current.copy(name = name, email = email, phoneNumber = phone)
        entrantRepository.saveOrUpdateEntrant(deviceId, updated)
    }

    // saves the entrant's notification settings
    fun updatePrefs(updated: Entrant) {
        entrantRepository.saveOrUpdateEntrant(deviceId, updated)
    }

    // deletes user profile from firebase
    fun deleteProfile() {
        entrantRepository.deleteEntrant(deviceId)
    }

    // Reservations (entrant)

    private val myReservations: List<Reservation>
        get() = reservationRepository.reservations.filter { it.userId == deviceId }

    // the entrant's active reservations
    val upcomingReservations: List<Reservation>
        get() = myReservations.upcoming()

    // the entrant's past reservations
    val pastReservations: List<Reservation>
        get() = myReservations.past()

    // lotteries the entrant won but hasn't picked dates for yet
    val bookingOffers: List<Lottery>
        get() = lotteryRepository.lotteries.filter { it.hasOpenOffer(deviceId) }

    fun offerCampsite(lottery: Lottery): Campsite? = lottery.entryFor(deviceId)?.campsiteId?.let(::campsite)

    // books the win: the winner picks one of the lottery's campsites and their dates.
    // The other campsites in the lottery are simply not taken.
    // TODO: let the owner know
    fun chooseDates(
        lottery: Lottery,
        campsiteId: String,
        checkIn: LocalDate,
        checkOut: LocalDate,
        onResult: (String?) -> Unit = {}
    ) {
        val reservation = Reservation(
            bookingId = UUID.randomUUID().toString(),
            campsiteId = campsiteId,
            userId = deviceId,
            lotteryId = lottery.id,
            checkIn = checkIn,
            checkOut = checkOut
        )
        // check so the user gets an instant message; claimWin re-checks everything on the server
        val site = campsite(campsiteId)
        if (site != null && (0 until reservation.nights).any { checkIn.plusDays(it.toLong()) in takenDates(site) }) {
            onResult("Some of those nights are already taken.")
            return
        }
        lotteryRepository.claimWin(lottery.id, reservation) { error ->
            if (error == null) {
                notificationRepository.send(
                    ReservationNotifications.confirmed(reservation, site?.name ?: "your campsite")
                )
            }
            onResult(error)
        }    }

    // give the lottery win up
    fun declineOffer(lottery: Lottery, onResult: (String?) -> Unit = {}) {
        lotteryRepository.declineWin(lottery.id, deviceId, onResult)
    }

    // marks the reservation cancelled and frees its nights, then sends the entrant
    // TODO: let the owner know
    fun cancelReservation(reservation: Reservation, onResult: (String?) -> Unit = {}) {
        reservationRepository.cancel(reservation, deviceId) { error ->
            if (error == null) {
                val name = campsite(reservation.campsiteId)?.name ?: "your campsite"
                notificationRepository.send(ReservationNotifications.cancelled(reservation, name))
            }
            onResult(error)
        }
    }

    // saves the rating on the reservation and updates the campsite's average (US 01.13.01)
    fun rate(reservation: Reservation, stars: Int, onResult: (String?) -> Unit = {}) {
        val problem = CampsiteRating.problem(reservation, stars, LocalDate.now())
        if (problem != null) {
            onResult(problem)
            return
        }
        reservationRepository.rate(reservation, stars, onResult)
    }

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
     * Uploads a newly picked photo to Firebase Storage, then writes the campsite with its URL. If
     * the photo can't be read or uploaded, [onError] gets a message and nothing is saved: a
     * device-local Uri is useless in Firestore. Photos that predate Firebase Storage are moved
     * there the next time their campsite is saved.
     */
    fun saveCampsite(campsite: Campsite, onSaved: () -> Unit = {}, onError: (String) -> Unit = {}) {
        val campsiteId = campsite.campsiteId.ifBlank { UUID.randomUUID().toString() }
        val ownerId = campsite.ownerId.ifBlank { deviceId }
        val base = campsite.copy(campsiteId = campsiteId, ownerId = ownerId)
        val image = base.imageUrl
        val storedInDocument = ImageUtils.decodeDataUri(image)

        if (image == null || (storedInDocument == null && !ImageUtils.isLocalUri(image))) {
            campsiteRepository.addCampsite(base)
            onSaved()
            return
        }

        fun fail(message: String, error: Exception) {
            Log.e("CortexViewModel", "$message (campsite $campsiteId)", error)
            // an image that was already saved like this shouldn't block other edits
            if (image == campsite(campsiteId)?.imageUrl) {
                campsiteRepository.addCampsite(base)
                onSaved()
            } else {
                onError(message)
            }
        }

        viewModelScope.launch {
            val jpeg = try {
                storedInDocument ?: ImageUtils.encodeAsJpeg(getApplication(), image)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fail("Couldn't use that image. Nothing was saved, please pick a different one.", e)
                return@launch
            }
            val uploaded = try {
                imageRepository.upload(campsiteId, jpeg)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fail("Couldn't upload the image. Nothing was saved, please try again.", e)
                return@launch
            }
            val replaced = campsite(campsiteId)?.imageUrl
            // whichever photo the campsite doesn't end up using is deleted
            campsiteRepository.addCampsite(base.copy(imageUrl = uploaded)) { error ->
                imageRepository.delete(if (error == null) replaced else uploaded)
            }
            onSaved()
        }
    }

    // TODO: save the new status on the entrant's reservation and notify them
    fun setReservationStatus(item: GuestReservation, status: ReservationStatus) {}

    // create the lottery (blank id) or save the changes
    fun saveLottery(lottery: Lottery, onResult: (String?) -> Unit = {}) {
        lotteryRepository.saveLottery(lottery.copy(ownerId = lottery.ownerId.ifBlank { deviceId }), onResult)
    }

    // Send one in-app notification per waitlisted user.
    fun notifyWaitlist(lottery: Lottery) {
        lottery.waitlist.filter { it.userId.isNotBlank() }.distinctBy { it.userId }.forEach { entry ->
            notificationRepository.send(
                AppNotification(
                    recipientId = entry.userId,
                    senderId = deviceId,
                    kind = NotificationKind.LOTTERY_UPDATED,
                    title = "Lottery waitlist update",
                    body = "You are on the waitlist for ${lottery.name}. Check the lottery for the latest details.",
                    link = "View lottery →",
                    campsiteId = entry.campsiteId,
                    target = NotificationTarget.CAMPSITE_LOTTERY,
                    forOwner = false
                )
            )
        }
    }

    // TODO: pick the winners, finalize the lottery and notify every entrant
    fun runDraw(lottery: Lottery) {}

    fun bookedDates(campsite: Campsite): Set<LocalDate> = takenDates(campsite)

    // Notifications

    // notifications sent to this device as an entrant, newest first
    val myNotifications: List<AppNotification>
        get() = notificationRepository.notifications
            .filter { it.recipientId == deviceId && !it.forOwner }
            .sortedByDescending { it.sentAt }

    // TODO: notifications sent to this device as an owner (forOwner = true)
    val ownerReceived: List<AppNotification> get() = emptyList()

    // TODO: notifications this owner sent
    val ownerSent: List<AppNotification> get() = emptyList()

    val unreadCount: Int get() = myNotifications.count { !it.read }
    val ownerUnreadCount: Int get() = ownerReceived.count { !it.read }


    // marks these notifications as read, skipping ones that already are
    fun markRead(notifications: List<AppNotification>) {
        notificationRepository.markRead(notifications.filter { !it.read }.map { it.id })
    }

    fun notificationsCsv(notifications: List<AppNotification>): String = buildString {
        appendLine("title,detail,link,sent,read")
        notifications.forEach { n ->
            appendLine(listOf(n.title, n.body, n.link, n.sentAt.toString(), n.read.toString())
                .joinToString(",") { "\"" + it.replace("\"", "\"\"") + "\"" })
        }
    }

    // Chat

    // TODO: messages between this entrant and the campsite's owner, oldest first
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
        moderationRepository.deleteCampsite(campsite.campsiteId) { error ->
            if (error == null) imageRepository.delete(campsite.imageUrl)
            onResult(error)
        }
    }

    val campsitesWithImages: List<Campsite> get() = campsites.filter { !it.imageUrl.isNullOrBlank() }

    // Remove only the reviewed image, without overwriting campsite edits.
    fun deleteImage(campsite: Campsite, onResult: (String?) -> Unit) {
        moderationRepository.deleteImage(campsite.campsiteId, campsite.imageUrl) { error ->
            if (error == null) imageRepository.delete(campsite.imageUrl)
            onResult(error)
        }
    }

    // Both repositories expose live state, so the admin log updates as activity arrives.
    val communicationLogs: List<CommunicationLog>
        get() {
            val messages = messageRepository.messages.map { message ->
                message.sentAt to CommunicationLog(
                    id = "message-${message.id.ifBlank { "${message.campsiteId}-${message.entrantId}-${message.sentAt}" }}",
                    type = LogType.Message,
                    subject = "Campsite ${message.campsiteId}",
                    route = "Room ${message.campsiteId}/${message.entrantId}",
                    meta = "Sender ${message.senderId} · Entrant ${message.entrantId} · ${message.sentAt}",
                    message = message.text
                )
            }
            val notifications = notificationRepository.notifications.map { notification ->
                notification.sentAt to CommunicationLog(
                    id = "notification-${notification.id}",
                    type = LogType.Notification,
                    subject = notification.title,
                    route = "Sender ${notification.senderId.ifBlank { "System" }} · Recipient ${notification.recipientId}",
                    meta = "${if (notification.forOwner) "Owner" else "Camper"} · ${notification.kind} · ${notification.sentAt} · ${if (notification.read) "Read" else "Unread"}",
                    message = notification.body
                )
            }
            return (messages + notifications)
                .sortedByDescending { it.first }
                .map { it.second }
        }
}
