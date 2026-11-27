const test = require("node:test");
const assert = require("node:assert/strict");
const {
  localNow, fromLocal, drawTask, scheduleDraw, isDue, pickWinners, drawNotifications, drawIfDue, drawDueLotteries,
} = require("../lib/draw");

const entries = (...ids) => ids.map((userId) => ({ userId, campsiteId: `site-${userId}` }));

test("localNow uses Edmonton time in LocalDateTime format", () => {
  assert.equal(localNow(new Date("2026-11-26T20:05:09Z")), "2026-11-26T13:05:09");
  assert.equal(localNow(new Date("2026-07-01T06:00:00Z")), "2026-07-01T00:00:00");
});

test("fromLocal reads Edmonton time, including across daylight saving", () => {
  assert.equal(fromLocal("2026-11-26T13:05:09").toISOString(), "2026-11-26T20:05:09.000Z");
  assert.equal(fromLocal("2026-07-01T00:00").toISOString(), "2026-07-01T06:00:00.000Z");
  assert.equal(fromLocal("2027-03-14T12:00").toISOString(), "2027-03-14T18:00:00.000Z");
  assert.equal(fromLocal("2026-11-27T14:00:30.123456").toISOString(), "2026-11-27T21:00:30.000Z");
});

test("a draw task runs at the draw time", () => {
  const { runAt, id } = drawTask("L1", "2026-11-27T14:00", new Date("2026-11-26T20:00:00Z"));
  assert.equal(runAt.toISOString(), "2026-11-27T21:00:00.000Z");
  assert.match(id, /^[A-Za-z0-9_-]+$/);
  assert.ok(id.startsWith("L1-"));
});

test("a draw more than 29 days away gets a task 29 days out", () => {
  const now = new Date("2026-11-26T20:00:00Z");
  const { runAt } = drawTask("L1", "2027-06-01T12:00", now);
  assert.equal(runAt.getTime() - now.getTime(), 29 * 24 * 60 * 60 * 1000);
});

test("rescheduling the draw gives the task a new id", () => {
  const now = new Date("2026-11-26T20:00:00Z");
  assert.notEqual(drawTask("L1", "2026-11-27T14:00", now).id, drawTask("L1", "2026-11-27T15:00", now).id);
  assert.equal(drawTask("L1", "2026-11-27T14:00", now).id, drawTask("L1", "2026-11-27T14:00", now).id);
});

test("scheduleDraw ignores a task that's already booked but not other errors", async () => {
  const booked = [];
  const queue = {
    enqueue: async (data, opts) => {
      if (booked.includes(opts.id)) throw Object.assign(new Error("exists"), { code: "functions/task-already-exists" });
      booked.push(opts.id);
    },
  };
  await scheduleDraw(queue, "L1", "2026-11-27T14:00");
  await scheduleDraw(queue, "L1", "2026-11-27T14:00");
  assert.equal(booked.length, 1);

  const broken = { enqueue: async () => { throw Object.assign(new Error("no"), { code: "functions/internal-error" }); } };
  await assert.rejects(scheduleDraw(broken, "L1", "2026-11-27T14:00"));
});

test("a lottery is due once its draw time has passed", () => {
  const now = "2026-11-26T14:00:00";
  assert.ok(isDue({ status: "ONGOING", drawAt: "2026-11-26T14:00" }, now));
  assert.ok(isDue({ status: "ONGOING", drawAt: "2026-11-26T13:59:59.5" }, now));
  assert.ok(!isDue({ status: "ONGOING", drawAt: "2026-11-26T14:01" }, now));
  assert.ok(!isDue({ status: "FINALIZED", drawAt: "2026-11-25T10:00" }, now));
  assert.ok(!isDue({ status: "ONGOING" }, now));
});

test("pickWinners picks the right number of different entrants", () => {
  const lottery = { winners: 3, entries: entries("a", "b", "c", "d", "e") };
  for (let i = 0; i < 50; i++) {
    const winners = pickWinners(lottery);
    assert.equal(winners.length, 3);
    assert.equal(new Set(winners).size, 3);
    winners.forEach((id) => assert.ok(["a", "b", "c", "d", "e"].includes(id)));
  }
});

test("everyone wins when there are fewer entrants than winners", () => {
  const winners = pickWinners({ winners: 5, entries: entries("a", "b", "a") });
  assert.deepEqual([...winners].sort(), ["a", "b"]);
});

test("no entrants means no winners", () => {
  assert.deepEqual(pickWinners({ winners: 3, entries: [] }), []);
  assert.deepEqual(pickWinners({ winners: 3 }), []);
});

test("winners, losers, the waitlist and the owner each get the right notification", () => {
  const lottery = {
    id: "L1", ownerId: "owner", name: "Summer", nights: 2,
    stayFrom: "2027-07-01", stayUntil: "2027-07-31",
    entries: entries("a", "b"), waitlist: entries("w"),
  };
  const byRecipient = Object.fromEntries(
    drawNotifications(lottery, ["a"], "2026-11-26T14:00:00").map((n) => [n.recipientId, n]),
  );

  assert.equal(byRecipient.a.kind, "BOOKING_OFFER");
  assert.equal(byRecipient.a.target, "CHOOSE_DATES");
  assert.equal(byRecipient.a.campsiteId, "site-a");
  assert.equal(byRecipient.a.id, "draw_L1_a");
  assert.equal(byRecipient.b.kind, "LOTTERY_UPDATED");
  assert.equal(byRecipient.w.target, "MY_LOTTERIES");
  assert.equal(byRecipient.owner.forOwner, true);
  assert.equal(byRecipient.owner.target, "OWNER_LOTTERIES");
  assert.equal(byRecipient.owner.body, "1 of 2 entries won.");
  for (const n of Object.values(byRecipient)) {
    assert.equal(n.sentAt, "2026-11-26T14:00:00");
    assert.equal(n.read, false);
    assert.equal(n.senderId, "");
  }
});

function fakeDb(lotteries) {
  const docs = new Map(Object.entries(lotteries));
  const notifications = new Map();
  const ref = (id) => ({ id, collection: "lotteries" });
  const snapshot = (id) => ({ id, exists: docs.has(id), data: () => docs.get(id), ref: ref(id) });
  return {
    docs, notifications,
    collection: (name) => ({
      where: (field, op, value) => ({
        get: async () => ({ docs: [...docs.keys()].filter((id) => docs.get(id)[field] === value).map(snapshot) }),
      }),
      doc: (id) => ({ id, collection: name }),
    }),
    runTransaction: async (fn) => fn({
      get: async (r) => snapshot(r.id),
      update: (r, fields) => docs.set(r.id, { ...docs.get(r.id), ...fields }),
    }),
    bulkWriter: () => ({ set: (r, data) => notifications.set(r.id, data), close: async () => {} }),
  };
}

test("drawIfDue only draws a lottery once its draw time has passed", async () => {
  const db = fakeDb({ L1: { status: "ONGOING", drawAt: "2026-11-26T12:00", winners: 1, entries: entries("a") } });
  assert.equal(await drawIfDue(db, "L1", new Date("2026-11-26T18:59:00Z")), false);
  assert.equal(db.docs.get("L1").status, "ONGOING");
  assert.equal(await drawIfDue(db, "L1", new Date("2026-11-26T19:00:00Z")), true);
  assert.deepEqual(db.docs.get("L1").winnerIds, ["a"]);
  assert.equal(await drawIfDue(db, "L1", new Date("2026-11-26T19:01:00Z")), false);
  assert.equal(await drawIfDue(db, "missing", new Date("2026-11-26T19:01:00Z")), false);
});

test("drawDueLotteries finalizes due lotteries once and leaves the rest alone", async () => {
  const db = fakeDb({
    due: { status: "ONGOING", drawAt: "2026-11-26T12:00", winners: 1, ownerId: "o", entries: entries("a", "b") },
    later: { status: "ONGOING", drawAt: "2026-11-27T12:00", winners: 1, entries: entries("c") },
    done: { status: "FINALIZED", drawAt: "2026-11-01T12:00", winners: 1, winnerIds: ["z"], entries: entries("z") },
  });
  const date = new Date("2026-11-26T20:00:00Z");

  assert.equal(await drawDueLotteries(db, date), 1);
  const due = db.docs.get("due");
  assert.equal(due.status, "FINALIZED");
  assert.equal(due.winnerIds.length, 1);
  assert.equal(db.docs.get("later").status, "ONGOING");
  assert.deepEqual(db.docs.get("done").winnerIds, ["z"]);
  assert.deepEqual([...db.notifications.keys()].sort(), ["draw_due_a", "draw_due_b", "draw_due_owner"]);

  assert.equal(await drawDueLotteries(db, date), 0);
  assert.deepEqual(db.docs.get("due").winnerIds, due.winnerIds);
});
