
/*
 * ReservationNotifications.kt
 *
 * Builds the notifications an entrant gets about their own reservations:
 * a confirmation when they book a stay, a confirmation when they cancel one,
 * and a notice when the campsite owner rejects one.
 *
 */

package com.example.cortex_camping.model
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Creates reservation confirmation notifications for the entrant who made the reservation.
 */
object ReservationNotifications {

    private val dateFormat = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)

    /**
     * Notification sent after a reservation is booked.
     *
     * @param reservation the reservation that was just booked
     * @param campsiteName name of the campsite, shown in the title
     * @return a notification for the entrant who owns the reservation
     */
    fun confirmed(reservation: Reservation, campsiteName: String): AppNotification =
        AppNotification(
            recipientId = reservation.userId,
            kind = NotificationKind.SUCCESS,
            title = "Reservation confirmed: $campsiteName",
            body = details(reservation),
            link = "View reservation →",
            campsiteId = reservation.campsiteId,
            target = NotificationTarget.MY_RESERVATIONS,
        )

    /**
     * Notification sent after a reservation is cancelled.
     *
     * @param reservation the reservation that was cancelled
     * @param campsiteName name of the campsite, shown in the title
     * @return a notification for the entrant who owns the reservation
     */
    fun cancelled(reservation: Reservation, campsiteName: String): AppNotification =
        AppNotification(
            recipientId = reservation.userId,
            kind = NotificationKind.CANCELLED,
            title = "Reservation cancelled: $campsiteName",
            body = details(reservation) + ". The nights have been released.",
            link = "View reservations →",
            campsiteId = reservation.campsiteId,
            target = NotificationTarget.MY_RESERVATIONS,
        )

    /**
     * Notification sent after the campsite owner rejects a reservation.
     *
     * @param reservation the reservation that was rejected, with the owner's reason if they gave one
     * @param campsiteName name of the campsite, shown in the title
     * @param winReopened true if the lottery win came back so the camper can pick new dates
     * @return a notification for the entrant who owns the reservation
     */
    fun rejected(reservation: Reservation, campsiteName: String, winReopened: Boolean): AppNotification =
        AppNotification(
            recipientId = reservation.userId,
            kind = NotificationKind.CANCELLED,
            title = "Reservation rejected: $campsiteName",
            body = details(reservation) +
                    reservation.rejectionReason.takeIf { it.isNotBlank() }?.let { ". Reason: $it" }.orEmpty() +
                    if (winReopened) ". Your lottery win is open again, so you can choose new dates." else "",
            link = if (winReopened) "Choose new dates →" else "View reservations →",
            campsiteId = reservation.campsiteId,
            target = NotificationTarget.MY_RESERVATIONS,
        )

    /**
     * The reservation details used in every notification
     *
     * @param reservation the reservation to describe
     * @return the dates, number of nights and guests, and booking ID
     */
    fun details(reservation: Reservation): String {
        val nights = reservation.nights
        val guests = reservation.guests
        return "${reservation.checkIn.format(dateFormat)} – ${reservation.checkOut.format(dateFormat)}" +
                " · $nights night${if (nights == 1) "" else "s"}" +
                " · $guests guest${if (guests == 1) "" else "s"}" +
                " · Booking ${reservation.bookingId}"
    }
}