# Feeder Calibration API

This document describes the **calibration workflow** for the cat feeder device.  
Calibration lets a user precisely measure how many grams per second the specific feeder motor dispenses, then saves that value to the device so all future feeding calculations are accurate.

> **Base URL:** `/feeder-service/api/...`  
> **Auth:** Bearer JWT token required on all endpoints.  
> **Required authority:** `manage-feeders`

---

## Overview

The calibration process works as follows:

```
1. [POST]   /calibration/start
        ↓  server fires feeder for 2000 ms
        ↓  returns sessionId + attemptId

2. User physically weighs the dispensed food

3. [POST]   /calibration/attempt   (submit weight)
        ↓  server fires feeder again (if attempts remain)
        ↓  repeat steps 2–3 until 5 attempts are complete

4. [GET]    /calibration/{sessionId}/result
        ↓  shows calculated feedConsumption + standard deviation

5. [POST]   /calibration/confirm   (accept or decline)
        ↓  on accept → device's feedConsumption is updated in DB

✕ [DELETE] /calibration/{sessionId}   (cancel at any point)
        ↓  aborts the session — device is left unchanged
```

The session is **in-memory only** — it is not persisted to the database and will be lost on server restart or after a **30-minute inactivity timeout**.

---

## Endpoints

### 1. Start Calibration

**`POST /calibration/start`**

Starts a new calibration session for a device and immediately fires the first feeding burst.  
If a session is **already active** for this device (`IN_PROGRESS` or `AWAITING_CONFIRMATION`), it is **automatically cancelled** before the new one begins — no manual cleanup needed.

#### Request body

```json
{
  "serialNumber": "ABC-12345"
}
```

| Field          | Type   | Required | Description                  |
|----------------|--------|----------|------------------------------|
| `serialNumber` | string | ✅       | Serial number of the device  |

#### Response `201 Created`

```json
{
  "sessionId": "1",
  "attemptId": "1",
  "attemptNumber": 1,
  "totalAttempts": 5,
  "calibrationDurationMs": 2000
}
```

| Field                   | Type   | Description                                              |
|-------------------------|--------|----------------------------------------------------------|
| `sessionId`             | string | ID of the calibration session (use in all further calls) |
| `attemptId`             | string | ID of the current attempt (required for submit)          |
| `attemptNumber`         | number | Current attempt index (starts at `1`)                    |
| `totalAttempts`         | number | Total number of attempts required (default: `5`)         |
| `calibrationDurationMs` | number | How long the motor ran in milliseconds (default: `2000`) |

#### Error responses

| Status | Reason                                        |
|--------|-----------------------------------------------|
| `404`  | Device with the given serial number not found |

---

### 2. Submit Attempt

**`POST /calibration/attempt`**

Submit the weight measured by the user for the current attempt.  
If more attempts remain, the server **automatically fires the next burst** and returns the next `attemptId`.  
When the final attempt is submitted, the server computes the result and returns `readyForConfirmation: true`.

#### Request body

```json
{
  "sessionId": "1",
  "attemptId": "1",
  "measuredGrams": 4.5
}
```

| Field           | Type   | Required | Description                                      |
|-----------------|--------|----------|--------------------------------------------------|
| `sessionId`     | string | ✅       | Session ID from the start response               |
| `attemptId`     | string | ✅       | Attempt ID from the previous response            |
| `measuredGrams` | number | ✅       | Weight of the dispensed food in grams (≥ 0)      |

#### Response `200 OK` — more attempts remain

```json
{
  "sessionId": "1",
  "nextAttemptId": "2",
  "attemptNumber": 2,
  "remainingAttempts": 3,
  "readyForConfirmation": false
}
```

#### Response `200 OK` — all attempts done

```json
{
  "sessionId": "1",
  "nextAttemptId": null,
  "attemptNumber": 5,
  "remainingAttempts": 0,
  "readyForConfirmation": true
}
```

| Field                  | Type           | Description                                                            |
|------------------------|----------------|------------------------------------------------------------------------|
| `sessionId`            | string         | Same session                                                           |
| `nextAttemptId`        | string \| null | ID to use in the next submit call (`null` when done)                   |
| `attemptNumber`        | number         | The attempt number just recorded                                       |
| `remainingAttempts`    | number         | How many weighings are still needed                                    |
| `readyForConfirmation` | boolean        | `true` when all attempts are done — proceed to `/result` + `/confirm` |

#### Error responses

| Status | Reason                                                        |
|--------|---------------------------------------------------------------|
| `400`  | Session not in `IN_PROGRESS` state / attempt already measured |
| `404`  | Session or attempt not found                                  |

---

### 3. Get Calibration Result

**`GET /calibration/{sessionId}/result`**

Returns the computed calibration values. Only available once all 5 attempts are submitted (`readyForConfirmation: true`).

#### Path parameter

| Param       | Description             |
|-------------|-------------------------|
| `sessionId` | ID from the start response |

#### Response `200 OK`

```json
{
  "sessionId": "1",
  "deviceSerialNumber": "ABC-12345",
  "calculatedFeedConsumption": 2.25,
  "standardDeviation": 0.08,
  "measurements": [2.1, 2.3, 2.2, 2.4, 2.2],
  "currentFeedConsumption": 2.0
}
```

| Field                      | Type     | Description                                                       |
|----------------------------|----------|-------------------------------------------------------------------|
| `sessionId`                | string   | Session ID                                                        |
| `deviceSerialNumber`       | string   | Device being calibrated                                           |
| `calculatedFeedConsumption`| number   | Computed average feed rate in **g/s**                             |
| `standardDeviation`        | number   | Std deviation across attempts in **g/s** — lower is more stable  |
| `measurements`             | number[] | Individual per-attempt g/s values                                 |
| `currentFeedConsumption`   | number   | The device's current g/s value saved in DB (before accepting)     |

#### Error responses

| Status | Reason                                              |
|--------|-----------------------------------------------------|
| `400`  | Session not yet ready (still collecting attempts) or expired |
| `404`  | Session not found                                   |

---

### 4. Confirm or Decline

**`POST /calibration/confirm`**

Accepts or declines the computed `feedConsumption`.  
- **Accept** → saves the new value to the device in the database.  
- **Decline** → discards the result; the device is unchanged.

#### Request body

```json
{
  "sessionId": "1",
  "accept": true
}
```

| Field       | Type    | Required | Description                             |
|-------------|---------|----------|-----------------------------------------|
| `sessionId` | string  | ✅       | Session ID                              |
| `accept`    | boolean | ✅       | `true` to save, `false` to discard      |

#### Response `204 No Content`

No body.

#### Error responses

| Status | Reason                                             |
|--------|----------------------------------------------------|
| `400`  | Session not in `AWAITING_CONFIRMATION` state       |
| `404`  | Session not found                                  |

---

### 5. Cancel Calibration

**`DELETE /calibration/{sessionId}`**

Cancels an active calibration session at any point — whether still collecting attempts or awaiting confirmation.  
The device's `feedConsumption` is **not** changed.  
After cancellation a fresh session can be started immediately with `POST /calibration/start`.

#### Path parameter

| Param       | Description                |
|-------------|----------------------------|
| `sessionId` | ID from the start response |

#### Response `204 No Content`

No body.

#### Error responses

| Status | Reason                                                         |
|--------|----------------------------------------------------------------|
| `400`  | Session is already finished (`ACCEPTED`, `DECLINED`, `EXPIRED`, `CANCELLED`) |
| `404`  | Session not found                                              |

---

## Full Flow Example

```
POST /calibration/start
  body: { "serialNumber": "ABC-12345" }
  → { sessionId: "1", attemptId: "1", attemptNumber: 1, totalAttempts: 5, calibrationDurationMs: 2000 }
  ← [feeder runs for 2s]

  User weighs food: 4.5g

POST /calibration/attempt
  body: { sessionId: "1", attemptId: "1", measuredGrams: 4.5 }
  → { nextAttemptId: "2", attemptNumber: 2, remainingAttempts: 3, readyForConfirmation: false }
  ← [feeder runs for 2s again]

  (repeat 3 more times...)

POST /calibration/attempt  (5th and final)
  body: { sessionId: "1", attemptId: "5", measuredGrams: 4.4 }
  → { nextAttemptId: null, attemptNumber: 5, remainingAttempts: 0, readyForConfirmation: true }

GET /calibration/1/result
  → { sessionId: "1", calculatedFeedConsumption: 2.22, standardDeviation: 0.06, measurements: [...], currentFeedConsumption: 2.0 }

POST /calibration/confirm
  body: { sessionId: "1", accept: true }
  → 204 No Content  ✓  device updated
```

---

## Session States

```
IN_PROGRESS  →  AWAITING_CONFIRMATION  →  ACCEPTED
                                       →  DECLINED
IN_PROGRESS  →  EXPIRED  (after 30 min of inactivity)
IN_PROGRESS  →  CANCELLED
AWAITING_CONFIRMATION  →  CANCELLED
```

| State                  | Meaning                                                      |
|------------------------|--------------------------------------------------------------|
| `IN_PROGRESS`          | Collecting attempts; accepts `POST /attempt`                 |
| `AWAITING_CONFIRMATION`| All attempts done; accepts `GET /result` and `POST /confirm` |
| `ACCEPTED`             | User confirmed; device updated                               |
| `DECLINED`             | User declined; device unchanged                              |
| `EXPIRED`              | Session timed out after 30 minutes                           |
| `CANCELLED`            | User explicitly cancelled; device unchanged                  |

> ⚠️ Sessions are **not persisted** — a server restart will clear all active sessions.  
> If a session expires or the server restarts, start a fresh calibration with `POST /calibration/start`.

---

## UI Recommendations

1. **Treat `sessionId` and `attemptId` as opaque strings** — they are serialized as JSON strings (not numbers) to avoid JavaScript `Number` precision loss. Always pass them back to the server exactly as received; never parse them as integers.
2. **Show a progress indicator** — e.g. `Attempt 2 / 5` using `attemptNumber` and `totalAttempts`.
3. **Countdown / instruction** — After firing a burst, show the user a prompt: *"The feeder just ran for 2 seconds. Please weigh the dispensed food and enter the amount."*
4. **Display the result page** with `calculatedFeedConsumption`, `standardDeviation`, the list of `measurements`, and the `currentFeedConsumption` so the user can compare before deciding.
5. **Low stdDev is good** — optionally show a quality indicator: stdDev < 0.1 g/s = ✅ Good, > 0.3 g/s = ⚠️ Inconsistent.
6. **Handle session expiry** — if a `400` is returned mentioning expiry, redirect the user back to the start screen.
7. **Restarting calibration is safe** — pressing "calibrate" again while a session is already active will automatically cancel the previous session and start fresh. You may still want to show a confirmation dialog (*"This will cancel the current calibration. Continue?"*) to avoid accidental resets.

