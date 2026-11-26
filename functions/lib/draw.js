const { randomInt } = require("node:crypto");

// The app saves lottery times as local times with no zone (LocalDateTime.toString()).
const TIME_ZONE = "America/Edmonton";

/** The current time in TIME_ZONE, formatted like LocalDateTime.toString(), e.g. "2026-11-26T14:05:09". */
function localNow(date = new Date()) {
  const parts = Object.fromEntries(
    new Intl.DateTimeFormat("en-CA", {
      timeZone: TIME_ZONE,
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
      hourCycle: "h23",
    })
      .formatToParts(date)
      .map((p) => [p.type, p.value]),
  );
  return `${parts.year}-${parts.month}-${parts.day}T${parts.hour}:${parts.minute}:${parts.second}`;
}

/** The moment a local TIME_ZONE time like "2026-11-26T14:05" happens. */
function fromLocal(local) {
  const [date, time = "00:00"] = local.split("T");
  const [year, month, day] = date.split("-").map(Number);
  const [hour, minute, second = "0"] = time.split(":");
  const target = Date.UTC(year, month - 1, day, Number(hour), Number(minute), Math.floor(Number(second)));
  let guess = target;
  for (let i = 0; i < 2; i++) guess += target - Date.parse(`${localNow(new Date(guess))}Z`);
  return new Date(guess);
}

// Cloud Tasks can't schedule more than 30 days ahead
const MAX_TASK_DELAY_MS = 29 * 24 * 60 * 60 * 1000;

/** When the next draw task should run and its id. A draw more than 29 days out gets a task that books the next one. */
function drawTask(lotteryId, drawAt, date = new Date()) {
  const drawTime = fromLocal(drawAt).getTime();
  const runAt = new Date(Math.min(drawTime, date.getTime() + MAX_TASK_DELAY_MS));
  const id = `${lotteryId}-${drawAt}-${runAt.getTime()}`.replace(/[^A-Za-z0-9_-]/g, "_");
  return { runAt, id };
}

/** Books a task to draw this lottery. Booking the same task twice is ignored. */
async function scheduleDraw(queue, lotteryId, drawAt, date = new Date()) {
  const { runAt, id } = drawTask(lotteryId, drawAt, date);
  try {
    await queue.enqueue({ lotteryId }, { scheduleTime: runAt, id });
  } catch (error) {
    if (error.code !== "functions/task-already-exists") throw error;
  }
}

/** ISO local times compare correctly as strings, even when one leaves out the seconds. */
function isDue(lottery, now) {
  return lottery.status === "ONGOING" && typeof lottery.drawAt === "string" && lottery.drawAt <= now;
}

/** Picks up to `lottery.winners` different entrants at random. */
function pickWinners(lottery, random = randomInt) {
  const ids = [...new Set((lottery.entries || []).map((e) => e.userId).filter(Boolean))];
  for (let i = ids.length - 1; i > 0; i--) {
    const j = random(i + 1);
    [ids[i], ids[j]] = [ids[j], ids[i]];
  }
  return ids.slice(0, Math.max(0, lottery.winners || 0));
}

function notification(id, fields, sentAt) {
  return {
    id,
    senderId: "",
    link: "",
    campsiteId: "",
    read: false,
    forOwner: false,
    sentAt,
    ...fields,
  };
}

/** One notification for every entrant, everyone on the waitlist and the owner. Ids are fixed so a rerun overwrites them. */
function drawNotifications(lottery, winnerIds, sentAt) {
  const name = lottery.name || "the lottery";
  const won = new Set(winnerIds);
  const seen = new Set();
  const out = [];

  for (const entry of lottery.entries || []) {
    if (!entry.userId || seen.has(entry.userId)) continue;
    seen.add(entry.userId);
    const id = `draw_${lottery.id}_${entry.userId}`;
    out.push(
      won.has(entry.userId)
        ? notification(id, {
            recipientId: entry.userId,
            kind: "BOOKING_OFFER",
            title: `You won the ${name} lottery!`,
            body: `Pick your campsite and dates for up to ${lottery.nights} nights between ${lottery.stayFrom} and ${lottery.stayUntil}.`,
            link: "Choose dates →",
            campsiteId: entry.campsiteId || "",
            target: "CHOOSE_DATES",
          }, sentAt)
        : notification(id, {
            recipientId: entry.userId,
            kind: "LOTTERY_UPDATED",
            title: `${name} results`,
            body: "You weren't picked this time. Thanks for entering!",
            link: "View lotteries →",
            campsiteId: entry.campsiteId || "",
            target: "MY_LOTTERIES",
          }, sentAt),
    );
  }

  for (const entry of lottery.waitlist || []) {
    if (!entry.userId || seen.has(entry.userId)) continue;
    seen.add(entry.userId);
    out.push(notification(`draw_${lottery.id}_${entry.userId}`, {
      recipientId: entry.userId,
      kind: "LOTTERY_UPDATED",
      title: `${name} results`,
      body: "The draw has happened and no spot opened up from the waitlist.",
      link: "View lotteries →",
      campsiteId: entry.campsiteId || "",
      target: "MY_LOTTERIES",
    }, sentAt));
  }

  if (lottery.ownerId) {
    const entrants = (lottery.entries || []).length;
    out.push(notification(`draw_${lottery.id}_owner`, {
      recipientId: lottery.ownerId,
      kind: "SUCCESS",
      title: `Draw complete for ${name}`,
      body: entrants === 0
        ? "No one entered, so there are no winners."
        : `${winnerIds.length} of ${entrants} entries won.`,
      link: "View lotteries →",
      target: "OWNER_LOTTERIES",
      forOwner: true,
    }, sentAt));
  }

  return out;
}

/** Draws one lottery if it's still due. Returns the notifications to send, or null if it was skipped. */
async function drawLottery(db, ref, now) {
  const result = await db.runTransaction(async (transaction) => {
    const snapshot = await transaction.get(ref);
    const lottery = { ...snapshot.data(), id: snapshot.id };
    if (!snapshot.exists || !isDue(lottery, now)) return null;
    const winnerIds = pickWinners(lottery);
    transaction.update(ref, { status: "FINALIZED", winnerIds });
    return { lottery, winnerIds };
  });
  if (!result) return null;
  return drawNotifications(result.lottery, result.winnerIds, now);
}

function sendNotifications(db, notifications) {
  const writer = db.bulkWriter();
  for (const n of notifications) writer.set(db.collection("notifications").doc(n.id), n);
  return writer.close();
}

/** Draws one lottery if its draw time has passed. Returns whether it was drawn. */
async function drawIfDue(db, lotteryId, date = new Date()) {
  const notifications = await drawLottery(db, db.collection("lotteries").doc(lotteryId), localNow(date));
  if (!notifications) return false;
  await sendNotifications(db, notifications);
  return true;
}

/** Draws every lottery whose draw time has passed. */
async function drawDueLotteries(db, date = new Date()) {
  const now = localNow(date);
  const ongoing = await db.collection("lotteries").where("status", "==", "ONGOING").get();
  const due = ongoing.docs.filter((doc) => isDue(doc.data(), now));

  let drawn = 0;
  for (const doc of due) {
    try {
      if (await drawIfDue(db, doc.id, date)) drawn++;
    } catch (error) {
      console.error(`Couldn't draw lottery ${doc.id}`, error);
    }
  }
  return drawn;
}

module.exports = {
  localNow,
  fromLocal,
  drawTask,
  scheduleDraw,
  isDue,
  pickWinners,
  drawNotifications,
  drawIfDue,
  drawDueLotteries,
  TIME_ZONE,
};
