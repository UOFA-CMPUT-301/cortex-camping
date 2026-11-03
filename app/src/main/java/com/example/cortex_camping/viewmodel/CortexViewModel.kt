package com.example.cortex_camping.viewmodel

import android.app.Application
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
import com.example.cortex_camping.ui.screens.admin.ManagedUser
import com.example.cortex_camping.ui.screens.owner.GuestReservation
import com.example.cortex_camping.util.DeviceUtils
import java.time.LocalDate
import com.example.cortex_camping.model.LotteryEntry
import com.example.cortex_camping.repository.LotteryRepository

enum class Role(val label: String) { User("Camper"), Owner("Campsite owner"), Admin("Admin") }

class CortexViewModel(application: Application) : AndroidViewModel(application) {

    val deviceId: String = DeviceUtils.getDeviceId(application)

    // TODO: return false until the entrant, owner and admin repositories have loaded,
    //  otherwise the app picks its start screen before it knows who the user is
    val isLoaded: Boolean get() = true

    var role by mutableStateOf(Role.User)

    // TODO: Admin if deviceId is in the admins collection, Owner if there's only an owner profile
    fun startingRole(): Role = Role.User

    // Current user

    // TODO: look up deviceId in EntrantRepository
    val entrant: Entrant? get() = null

    // TODO: look up deviceId in CampsiteOwnerRepository
    val owner: CampsiteOwner? get() = null

    // TODO: check AdminRepository
    val isAdmin: Boolean get() = false

    val isRegistered: Boolean get() = entrant != null

    // TODO: look up the name in EntrantRepository
    fun entrantName(id: String): String = "Camper"

    // TODO: look up the name in CampsiteOwnerRepository
    fun ownerName(id: String): String = "Campsite owner"

    // Campsites

    // TODO: CampsiteRepository.campsites
    val campsites: List<Campsite> get() = emptyList()

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

    // TODO: campsites where ownerId == deviceId
    val myCampsites: List<Campsite> get() = emptyList()

    // lotteries where ownerId == deviceId
    val ownerLotteries: List<Lottery>
        get() = lotteryRepository.lotteries.filter {
            it.ownerId == deviceId
        }

    fun hasOngoingLottery(campsite: Campsite): Boolean = false

    // TODO: every entrant's reservations at the owner's campsites, with the guest's name
    val ownerReservations: List<GuestReservation> get() = emptyList()

    // TODO: CampsiteRepository.addCampsite for new campsites (blank campsiteId), editCampsite otherwise
    fun saveCampsite(campsite: Campsite) {}

    // TODO: save the new status on the entrant's reservation and notify them
    fun setReservationStatus(item: GuestReservation, status: ReservationStatus) {}

    // create the lottery (blank id) or save the changes
    fun saveLottery(lottery: Lottery) {
        lotteryRepository.saveLottery(lottery)
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

    // TODO: messages between this entrant and the campsite's owner, oldest first
    fun conversation(campsiteId: String, entrantId: String): List<Message> = emptyList()

    // TODO: save the message and notify the other person
    fun sendMessage(campsiteId: String, entrantId: String, text: String) {}

    // Admin

    // TODO: every entrant and owner
    val managedUsers: List<ManagedUser> get() = emptyList()

    // TODO: delete the entrant or owner profile
    fun deleteUser(user: ManagedUser) {}

    // TODO: delete the campsite
    fun deleteCampsite(campsite: Campsite) {}

    val campsitesWithImages: List<Campsite> get() = campsites.filter { !it.imageUrl.isNullOrBlank() }

    // TODO: clear the campsite's imageUrl
    fun deleteImage(campsite: Campsite) {}

    // TODO: every notification and message, newest first
    val communicationLogs: List<CommunicationLog> get() = emptyList()
}
