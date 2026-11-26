const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");
const { setGlobalOptions } = require("firebase-functions");
const { onDocumentCreated } = require("firebase-functions/firestore");
const { sendPush } = require("./lib/push");

initializeApp();
const db = getFirestore();

// The Firestore trigger has to be in a region that matches the database's location.
setGlobalOptions({ region: "us-central1", maxInstances: 5 });

/** Pushes every new notification to its recipient's phone. */
exports.pushNotification = onDocumentCreated("notifications/{id}", async (event) => {
  if (event.data) await sendPush(db, getMessaging(), event.params.id, event.data.data());
});
