const test = require("node:test");
const assert = require("node:assert/strict");
const { allowsPush, pushMessage } = require("../lib/push");

test("settings silence the right pushes", () => {
  const win = { kind: "BOOKING_OFFER", target: "CHOOSE_DATES" };
  const chat = { kind: "MESSAGE", target: "CHAT" };
  const booking = { kind: "SUCCESS", target: "MY_RESERVATIONS" };
  assert.ok(allowsPush(undefined, win));
  assert.ok(allowsPush(undefined, chat));
  assert.ok(!allowsPush(undefined, booking));
  assert.ok(allowsPush({ reservationNotifications: true }, booking));
  assert.ok(!allowsPush({ lotteryNotifications: false }, win));
  assert.ok(!allowsPush({ lotteryNotifications: false }, { kind: "NEUTRAL", target: "MY_LOTTERIES" }));
  assert.ok(!allowsPush({ notificationsEnabled: false }, chat));
  assert.ok(allowsPush({ notificationsEnabled: false }, { ...chat, forOwner: true }));
});

test("push data is all strings", () => {
  const { data, android } = pushMessage("fid", "n1", { title: "t", body: "b", forOwner: true });
  for (const value of Object.values(data)) assert.equal(typeof value, "string");
  assert.equal(data.forOwner, "true");
  assert.equal(android.notification.tag, "n1");
});
