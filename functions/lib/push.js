const CHANNEL_ID = "updates"; // notification_channel_id in the app's strings.xml

const LOTTERY_KINDS = ["BOOKING_OFFER", "LOTTERY_UPDATED"];
const LOTTERY_TARGETS = ["MY_LOTTERIES", "CAMPSITE_LOTTERY"];

/**
 * Whether an entrant's settings (Entrant.kt) allow this push. The defaults match the model's. The
 * settings only silence the phone: the notification is always in the app's own feed.
 */
function allowsPush(entrant, notification) {
  if (notification.forOwner) return true; // owners have no notification settings
  const {
    notificationsEnabled = true,
    lotteryNotifications = true,
    reservationNotifications = false,
  } = entrant || {};
  if (!notificationsEnabled) return false;
  if (LOTTERY_KINDS.includes(notification.kind) || LOTTERY_TARGETS.includes(notification.target)) {
    return lotteryNotifications;
  }
  if (notification.target === "MY_RESERVATIONS") return reservationNotifications;
  return true;
}

function pushMessage(fid, id, notification) {
  return {
    fid,
    notification: { title: notification.title || "", body: notification.body || "" },
    // data values must be strings
    data: {
      notificationId: id,
      target: notification.target || "NONE",
      campsiteId: notification.campsiteId || "",
      senderId: notification.senderId || "",
      forOwner: String(notification.forOwner === true),
    },
    android: {
      priority: "high",
      // triggers can run twice, and a second push with the same tag replaces the first
      notification: { channelId: CHANNEL_ID, tag: id },
    },
  };
}

/** Sends a notification document to its recipient's phone, if they have one and want it. */
async function sendPush(db, messaging, id, notification, log = console) {
  if (!notification.recipientId) {
    log.info(`Not pushing ${id}: it has no recipientId`);
    return false;
  }
  const deviceRef = db.collection("devices").doc(notification.recipientId);
  const [device, entrant] = await Promise.all([
    deviceRef.get(),
    db.collection("entrants").doc(notification.recipientId).get(),
  ]);
  const fid = device.get("fid");
  if (!fid) {
    log.info(`Not pushing ${id}: devices/${notification.recipientId} has no fid`);
    return false;
  }
  if (!allowsPush(entrant.data(), notification)) {
    log.info(`Not pushing ${id}: ${notification.recipientId}'s settings turn it off`);
    return false;
  }
  try {
    await messaging.send(pushMessage(fid, id, notification));
    log.info(`Pushed ${id} to ${notification.recipientId}`);
    return true;
  } catch (error) {
    if (!/(installation-id|registration-token)-not-registered$/.test(error.code || "")) throw error;
    log.info(`Removing the uninstalled device ${notification.recipientId}`);
    // unless the app has registered again in the meantime
    await db.runTransaction(async (transaction) => {
      const current = await transaction.get(deviceRef);
      if (current.get("fid") === fid) transaction.delete(deviceRef);
    });
    return false;
  }
}

module.exports = { allowsPush, pushMessage, sendPush };
