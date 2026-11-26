// Runs against the Firestore emulator: npm run test:emulator
const test = require("node:test");
const assert = require("node:assert/strict");
if (!process.env.FIRESTORE_EMULATOR_HOST) throw new Error("Only run this against the emulator.");
const { getFirestore } = require("firebase-admin/firestore");
require("../index"); // initializes the admin app
const { sendPush } = require("../lib/push");

const db = getFirestore();
const silent = { info() {}, error() {} };

test("pushes go to the recipient's device and uninstalled devices are removed", async () => {
  const sent = [];
  const messaging = { send: async (m) => sent.push(m) };
  const gone = { send: async () => { throw Object.assign(new Error(), { code: "messaging/installation-id-not-registered" }); } };
  const broken = { send: async () => { throw Object.assign(new Error("down"), { code: "messaging/internal-error" }); } };
  const n = { recipientId: "phone", kind: "MESSAGE", target: "CHAT", title: "Hi" };
  assert.equal(await sendPush(db, messaging, "n", n, silent), false); // no device yet
  const device = db.collection("devices").doc("phone");
  await device.set({ fid: "fid-1" });
  assert.equal(await sendPush(db, messaging, "n", n, silent), true);
  assert.equal(sent[0].fid, "fid-1");
  await db.collection("entrants").doc("phone").set({ notificationsEnabled: false });
  assert.equal(await sendPush(db, messaging, "n", n, silent), false);
  await db.collection("entrants").doc("phone").delete();
  await assert.rejects(sendPush(db, broken, "n", n, silent));
  assert.ok((await device.get()).exists);
  await sendPush(db, gone, "n", n, silent);
  assert.ok(!(await device.get()).exists);
});
