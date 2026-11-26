const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getFunctions } = require("firebase-admin/functions");
const { getMessaging } = require("firebase-admin/messaging");
const { setGlobalOptions } = require("firebase-functions");
const { onDocumentCreated, onDocumentWritten } = require("firebase-functions/firestore");
const { onSchedule } = require("firebase-functions/scheduler");
const { onTaskDispatched } = require("firebase-functions/tasks");
const { sendPush } = require("./lib/push");
const { drawDueLotteries, drawIfDue, isDue, localNow, scheduleDraw, TIME_ZONE } = require("./lib/draw");

initializeApp();
const db = getFirestore();

// The Firestore trigger has to be in a region that matches the database's location.
setGlobalOptions({ region: "us-central1", maxInstances: 5 });

/** Pushes every new notification to its recipient's phone. */
exports.pushNotification = onDocumentCreated("notifications/{id}", async (event) => {
  if (event.data) await sendPush(db, getMessaging(), event.params.id, event.data.data());
});

const drawQueue = () => getFunctions().taskQueue("drawLottery");

/** Books a draw task whenever a lottery is created or its draw time changes. */
exports.scheduleLotteryDraw = onDocumentWritten("lotteries/{id}", async (event) => {
  const before = event.data.before.data();
  const after = event.data.after.data();
  if (!after || after.status !== "ONGOING" || !after.drawAt) return;
  if (before && before.status === "ONGOING" && before.drawAt === after.drawAt) return;
  await scheduleDraw(drawQueue(), event.params.id, after.drawAt);
});

/** Draws one lottery at its draw time. Winners and entrants are notified through pushNotification. */
exports.drawLottery = onTaskDispatched({ retryConfig: { maxAttempts: 5, minBackoffSeconds: 30 } }, async (request) => {
  const { lotteryId } = request.data;
  if (await drawIfDue(db, lotteryId)) return;
  const lottery = (await db.collection("lotteries").doc(lotteryId).get()).data();
  if (lottery && lottery.status === "ONGOING" && lottery.drawAt && !isDue(lottery, localNow())) {
    await scheduleDraw(drawQueue(), lotteryId, lottery.drawAt);
  }
});

/** Catches any lottery whose draw task didn't run, such as ones created before the tasks existed. */
exports.drawLotteries = onSchedule({ schedule: "every 1 hours", timeZone: TIME_ZONE }, async () => {
  const drawn = await drawDueLotteries(db);
  if (drawn > 0) console.log(`Drew ${drawn} overdue lotteries`);
});
