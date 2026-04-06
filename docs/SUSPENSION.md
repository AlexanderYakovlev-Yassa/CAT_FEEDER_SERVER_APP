# Device Suspension API

This document describes the **suspension feature** for the cat feeder.  
A suspension tells the server to **skip all scheduled feedings** for a specific device during a defined time window — for example, while the owner is home and feeding the cat manually, or during a vet visit.

> **Base URL:** `/feeder-service/api/...`  
> **Auth:** Bearer JWT token required on all endpoints.  
> **Required authority:** `read-schedule` (read), `manage-schedule` (create / update / delete)

---

## How It Works

```
Client                                    Server
  |                                          |
  |  POST /suspensions                       |
  |  { deviceId, startSuspension,            |
  |    endSuspension }  ------------------> |
  |                                          |  Saves suspension window to DB
  |                                          |  Reschedules all feeding tasks
  |  <-- 201 { id, deviceId, ... }          |
  |                                          |
  |              [ 2026-04-10 08:00 UTC ]   |
  |                                          |  Scheduler fires feeding task
  |                                          |  ↳ checks DB for active suspension
  |                                          |  ↳ suspension found → feeding SKIPPED
  |                                          |
  |              [ 2026-04-13 08:00 UTC ]   |
  |                                          |  Suspension has ended
  |                                          |  ↳ feeding executes normally
```

### Key rules
- A suspension is **inclusive** on both ends: `startSuspension ≤ now ≤ endSuspension`.
- Multiple suspensions can exist for the same device at the same time.
- Suspensions are **persisted** in the database — they survive server restarts.
- Creating, updating, or deleting a suspension **immediately reschedules** all feeding tasks.
- Deleting a suspension **restores** feedings for that device (for the remainder of the current day).

---

## Timestamps — Unix Epoch Seconds (UTC)

> ⚠️ **All timestamps in this API are Unix epoch seconds in UTC.**  
> Do **not** send local date-time strings. Different clients in different timezones sending the same string (e.g. `"2026-04-10T08:00:00"`) would mean completely different moments in time.

**Unix timestamp** is always unambiguous: `1744272000` means `2026-04-10 08:00:00 UTC` for every client in every timezone.

### How to get the current Unix timestamp

```js
// JavaScript
const now = Math.floor(Date.now() / 1000);

// Kotlin / Android
val now = Instant.now().epochSecond

// Python
import time; now = int(time.time())

// Swift
let now = Int(Date().timeIntervalSince1970)
```

### How to build a suspension for a specific local time

Always convert from the user's **local time + their timezone** to UTC before sending:

```js
// JavaScript — suspend from April 10 08:00 to April 13 20:00 in Warsaw time (UTC+2)
const start = new Date('2026-04-10T08:00:00+02:00');
const end   = new Date('2026-04-13T20:00:00+02:00');

const body = {
  deviceId:        1,
  startSuspension: Math.floor(start.getTime() / 1000),  // → 1744264800
  endSuspension:   Math.floor(end.getTime()   / 1000),  // → 1744574400
};
```

```kotlin
// Kotlin / Android — using ZonedDateTime
val zone  = ZoneId.of("Europe/Warsaw")
val start = ZonedDateTime.of(2026, 4, 10, 8, 0, 0, 0, zone).toEpochSecond()
val end   = ZonedDateTime.of(2026, 4, 13, 20, 0, 0, 0, zone).toEpochSecond()
```

---

## Endpoints

### 1. Create a Suspension

**`POST /suspensions`**

Registers a new suspension window for a device.  
The scheduler is **immediately rescheduled** — any feeding task already queued inside the window is cancelled right away.

#### Request body

```json
{
  "deviceId":        1,
  "startSuspension": 1744264800,
  "endSuspension":   1744574400
}
```

| Field             | Type   | Required | Description                                               |
|-------------------|--------|----------|-----------------------------------------------------------|
| `deviceId`        | number | ✅       | ID of the device to suspend                               |
| `startSuspension` | number | ✅       | Start of the suspension window — **Unix epoch seconds, UTC** |
| `endSuspension`   | number | ✅       | End of the suspension window — **Unix epoch seconds, UTC**   |

#### Response `201 Created`

```json
{
  "id":              7,
  "deviceId":        1,
  "startSuspension": 1744264800,
  "endSuspension":   1744574400
}
```

| Field             | Type   | Description                              |
|-------------------|--------|------------------------------------------|
| `id`              | number | DB identifier of the created suspension  |
| `deviceId`        | number | Device that is suspended                 |
| `startSuspension` | number | Start of window (Unix epoch seconds, UTC)|
| `endSuspension`   | number | End of window (Unix epoch seconds, UTC)  |

#### Error responses

| Status | Reason                                          |
|--------|-------------------------------------------------|
| `400`  | Missing required field or validation failure    |
| `401`  | Missing or invalid JWT token                    |
| `403`  | Insufficient authority (`manage-schedule` needed) |

---

### 2. List All Suspensions

**`GET /suspensions`**

Returns every suspension in the system across all devices.

#### Response `200 OK`

```json
[
  {
    "id":              7,
    "deviceId":        1,
    "startSuspension": 1744264800,
    "endSuspension":   1744574400
  },
  {
    "id":              8,
    "deviceId":        2,
    "startSuspension": 1744300000,
    "endSuspension":   1744400000
  }
]
```

---

### 3. List Suspensions for a Device

**`GET /suspensions/device/{deviceId}`**

Returns only the suspensions registered for a specific device.  
Useful for displaying the suspension schedule on a per-device settings screen.

#### Path parameter

| Param      | Description            |
|------------|------------------------|
| `deviceId` | ID of the target device |

#### Response `200 OK`

```json
[
  {
    "id":              7,
    "deviceId":        1,
    "startSuspension": 1744264800,
    "endSuspension":   1744574400
  }
]
```

An **empty array** `[]` means the device has no suspensions configured.

---

### 4. Get a Single Suspension

**`GET /suspensions/{id}`**

#### Path parameter

| Param | Description                       |
|-------|-----------------------------------|
| `id`  | ID of the suspension (from create) |

#### Response `200 OK`

```json
{
  "id":              7,
  "deviceId":        1,
  "startSuspension": 1744264800,
  "endSuspension":   1744574400
}
```

#### Error responses

| Status | Reason                  |
|--------|-------------------------|
| `404`  | Suspension ID not found |

---

### 5. Update a Suspension

**`PUT /suspensions/{id}`**

Replaces all fields of an existing suspension.  
The scheduler is **immediately rescheduled** after the update.

#### Path parameter

| Param | Description               |
|-------|---------------------------|
| `id`  | ID of the suspension to update |

#### Request body

Same structure as `POST /suspensions`:

```json
{
  "deviceId":        1,
  "startSuspension": 1744264800,
  "endSuspension":   1744660800
}
```

#### Response `200 OK`

Returns the updated suspension object.

#### Error responses

| Status | Reason                                            |
|--------|---------------------------------------------------|
| `400`  | Validation failure                                |
| `404`  | Suspension ID not found                           |

---

### 6. Delete a Suspension

**`DELETE /suspensions/{id}`**

Permanently removes a suspension window.  
The scheduler is **immediately rescheduled** — feedings that were suppressed by this suspension will now execute for the remainder of the current day (if their scheduled time hasn't passed yet).

#### Path parameter

| Param | Description               |
|-------|---------------------------|
| `id`  | ID of the suspension to delete |

#### Response `204 No Content`

No body.

#### Error responses

| Status | Reason                  |
|--------|-------------------------|
| `404`  | Suspension ID not found |

---

## Full Flow Examples

### cURL

```bash
TOKEN="eyJhbGci..."

# Create a suspension (April 10–13, Warsaw UTC+2)
curl -X POST http://localhost:8080/feeder-service/api/suspensions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "deviceId":        1,
    "startSuspension": 1744264800,
    "endSuspension":   1744574400
  }'

# List suspensions for device 1
curl -X GET http://localhost:8080/feeder-service/api/suspensions/device/1 \
  -H "Authorization: Bearer $TOKEN"

# Delete suspension with id 7
curl -X DELETE http://localhost:8080/feeder-service/api/suspensions/7 \
  -H "Authorization: Bearer $TOKEN"
```

### JavaScript (fetch)

```js
const BASE = 'http://localhost:8080/feeder-service/api';
const token = '...';
const headers = {
  'Authorization': `Bearer ${token}`,
  'Content-Type': 'application/json',
};

// --- Helper: convert a local Date to epoch seconds ---
const toEpoch = (date) => Math.floor(date.getTime() / 1000);

// --- Create suspension ---
const start = new Date('2026-04-10T08:00:00+02:00'); // user's local time in Warsaw
const end   = new Date('2026-04-13T20:00:00+02:00');

const suspension = await fetch(`${BASE}/suspensions`, {
  method: 'POST',
  headers,
  body: JSON.stringify({
    deviceId:        1,
    startSuspension: toEpoch(start),
    endSuspension:   toEpoch(end),
  }),
}).then(res => res.json());

console.log('Created suspension id:', suspension.id);

// --- List suspensions for device 1 ---
const list = await fetch(`${BASE}/suspensions/device/1`, { headers })
  .then(res => res.json());

// --- Delete a suspension ---
await fetch(`${BASE}/suspensions/${suspension.id}`, {
  method: 'DELETE',
  headers,
});
```

### Kotlin / Android

```kotlin
// Convert back for display: epoch → ZonedDateTime in user's timezone
fun epochToLocal(epochSecond: Long, zoneId: ZoneId): ZonedDateTime =
    Instant.ofEpochSecond(epochSecond).atZone(zoneId)

// Create suspension
val zone  = ZoneId.of("Europe/Warsaw")
val start = ZonedDateTime.of(2026, 4, 10, 8, 0, 0, 0, zone).toEpochSecond()
val end   = ZonedDateTime.of(2026, 4, 13, 20, 0, 0, 0, zone).toEpochSecond()

val response = api.createSuspension(
    DeviceSuspensionRequest(
        deviceId        = 1L,
        startSuspension = start,
        endSuspension   = end,
    )
)
```

---

## Displaying Timestamps in the UI

The API returns raw epoch seconds — convert them to the **user's local timezone** for display:

```js
// JavaScript
function formatEpoch(epochSeconds, timeZone) {
  return new Date(epochSeconds * 1000).toLocaleString('en-US', { timeZone });
}

// Example: "4/10/2026, 10:00:00 AM" for Warsaw user (UTC+2)
formatEpoch(1744264800, 'Europe/Warsaw');
```

```kotlin
// Kotlin / Android
val userZone   = ZoneId.of("Europe/Warsaw")
val displayTime = Instant.ofEpochSecond(epochSecond).atZone(userZone)
val formatted  = displayTime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))
```

> 💡 **Tip:** Always store the user's preferred timezone in app settings and use it consistently for both display and conversion. Never hardcode a timezone.

---

## UI Recommendations

1. **Date-time picker should be in the user's local time** — convert to epoch seconds only at the moment you call the API. Show the local time back to the user when displaying existing suspensions.
2. **Show suspensions in a list per device** — use `GET /suspensions/device/{deviceId}` on the device detail screen.
3. **Confirm before deleting** — removing a suspension restores automatic feedings immediately; show a dialog: *"Remove suspension? Feedings will resume for this device."*
4. **Indicate active suspensions** — if `now` falls between `startSuspension` and `endSuspension`, the device is currently suspended. You can compute this client-side: `startSuspension <= Math.floor(Date.now()/1000) <= endSuspension`.
5. **Validate before submitting** — check that `endSuspension > startSuspension` on the client before sending the request.
6. **Handle overlapping suspensions** — the server allows multiple overlapping windows for the same device. If your UI should prevent this, check the existing list before creating a new one.

