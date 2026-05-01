# Camera API — Frontend Developer Guide

Base URL: `/feeder-service/api`  
All requests require a **Bearer JWT** token in the `Authorization` header.

---

## Table of Contents

1. [Data Models](#data-models)
2. [Camera Management (admin)](#camera-management-admin)
3. [User–Camera Association](#usercamera-association)
4. [Recording Control (admin)](#recording-control-admin)
5. [Live HLS Stream](#live-hls-stream)
6. [VOD (Historical Playback)](#vod-historical-playback)
7. [Segment File Serving](#segment-file-serving)
8. [Recording Browser](#recording-browser)
9. [Required Authorities](#required-authorities)
10. [React / hls.js Quick-Start](#react--hlsjs-quick-start)

---

## Data Models

### `CameraDto`

```json
{
  "id":          1,
  "name":        "CatCamMaster",
  "rtspUrl":     "rtsp://user:pass@192.168.1.100:554/stream",
  "storagePath": "/recordings/CatCamMaster",
  "autoStart":   true,
  "userIds":     [3, 7]
}
```

| Field         | Type      | Description                                                  |
|---------------|-----------|--------------------------------------------------------------|
| `id`          | `number`  | Database primary key (set by server on create)               |
| `name`        | `string`  | Unique logical name; used in all stream/segment URL paths    |
| `rtspUrl`     | `string`  | Full RTSP URL (including credentials)                        |
| `storagePath` | `string`  | Absolute server path where `.ts` segment files are written   |
| `autoStart`   | `boolean` | Auto-start recording on app startup / process restart        |
| `userIds`     | `number[]`| IDs of users who have access to this camera                  |

### `RecordingDayDto`

```json
{
  "date":           "2026-04-26",
  "segmentCount":   6,
  "totalSizeBytes": 943718400,
  "segments": [ /* SegmentInfoDto[] */ ]
}
```

### `SegmentInfoDto`

```json
{
  "filename":  "CatCamMaster_2026-04-26_14-30-00.ts",
  "date":      "2026-04-26",
  "startTime": "14:30:00",
  "sizeBytes": 157286400
}
```

---

## Camera Management (admin)

> **Required authority:** `manage-schedule`

### List all cameras
```
GET /cameras
```
**Response:** `200 OK` — `CameraDto[]`

---

### Get camera by ID
```
GET /cameras/{id}
```
**Response:** `200 OK` — `CameraDto` | `404 Not Found`

---

### Create camera
```
POST /cameras
Content-Type: application/json

{
  "name":        "CatCamMaster",
  "rtspUrl":     "rtsp://user:pass@192.168.1.100:554/stream",
  "storagePath": "/recordings/CatCamMaster",
  "autoStart":   false,
  "userIds":     []
}
```
**Response:** `201 Created` — `CameraDto` (with `id`)  
`Location` header: `/feeder-service/api/cameras/{id}`

---

### Update camera
```
PUT /cameras/{id}
Content-Type: application/json
```
Body: same shape as create.  
**Response:** `200 OK` — updated `CameraDto`

---

### Delete camera
```
DELETE /cameras/{id}
```
**Response:** `204 No Content`

---

## User–Camera Association

> **Required authority:** `manage-schedule` (write) / `read-schedule` (read)

### List cameras for a user
```
GET /users/{userId}/cameras
```
**Response:** `200 OK` — `CameraDto[]`

---

### Assign camera to user
```
POST /cameras/{id}/users/{userId}
```
or equivalently:
```
POST /users/{userId}/cameras/{cameraId}
```
**Response:** `200 OK` — updated `CameraDto`

---

### Remove camera from user
```
DELETE /cameras/{id}/users/{userId}
```
or equivalently:
```
DELETE /users/{userId}/cameras/{cameraId}
```
**Response:** `204 No Content`

---

## Recording Control (admin)

> **Required authority:** `manage-schedule`

The server uses **FFmpeg** to record the RTSP stream into `.ts` segment files.

### Start recording
```
POST /camera/{name}/start
```
Starts the FFmpeg recording process. Silent no-op if already recording.  
**Response:** `200 OK` — plain text `"Recording started for camera: {name}"`

---

### Stop recording
```
POST /camera/{name}/stop
```
Stops the FFmpeg process. The last open segment is truncated at stop time.  
**Response:** `200 OK` — plain text `"Recording stopped for camera: {name}"`

---

### Get recording status (all cameras)
```
GET /camera/status
```
> **Required authority:** `read-schedule`

**Response:** `200 OK`
```json
{
  "CatCamMaster": true,
  "GardenCam":    false
}
```
`true` = currently recording, `false` = stopped.

---

## Live HLS Stream

> **Required authority:** `read-schedule`

```
GET /camera/{name}/live/playlist.m3u8
```

Returns an **HLS EVENT playlist** (sliding window of the most-recently completed segments).  
`Cache-Control: no-cache, no-store` — the player must poll this URL continuously.

The response body is a standard M3U8 playlist; segment URLs inside it are absolute paths pointing to the [segment endpoint](#segment-file-serving).

---

## VOD (Historical Playback)

> **Required authority:** `read-schedule`

```
GET /camera/{name}/vod/playlist.m3u8?date=2026-04-26[&from=14:00&to=16:00]
```

| Query param | Required | Format  | Description                                   |
|-------------|----------|---------|-----------------------------------------------|
| `date`      | ✅        | `yyyy-MM-dd` | Day to play back                         |
| `from`      | ❌        | `HH:mm` | Inclusive start time filter (default: 00:00)  |
| `to`        | ❌        | `HH:mm` | Exclusive end time filter (default: end of day) |

**Response:** `200 OK` — closed HLS VOD M3U8 playlist  
`Cache-Control: public, max-age=60`

---

## Segment File Serving

> **Required authority:** `read-schedule`

```
GET /camera/{name}/segments/{filename}
```

| Path param | Example                                    |
|------------|--------------------------------------------|
| `name`     | `CatCamMaster`                             |
| `filename` | `CatCamMaster_2026-04-26_14-30-00.ts`      |

**Response:** `200 OK` — `video/MP2T` binary stream  
Supports **HTTP Range requests** — the player can seek within a segment.  
`Cache-Control: public, max-age=3600`

> **Note:** You normally don't call this endpoint directly. hls.js resolves segment URLs from the playlist automatically.

---

## Recording Browser

> **Required authority:** `read-schedule`

### List recording days
```
GET /camera/{name}/recordings
```
Returns all days that have at least one recording, newest first.  
**Response:** `200 OK` — `RecordingDayDto[]`

---

### List segments for a day
```
GET /camera/{name}/recordings/{date}
```
`date` format: `yyyy-MM-dd` (e.g. `2026-04-26`)  
**Response:** `200 OK` — `SegmentInfoDto[]` (ascending by start time)

---

## Required Authorities

| Authority         | Description                                         |
|-------------------|-----------------------------------------------------|
| `read-schedule`   | View cameras, live/VOD stream, recording browser    |
| `manage-schedule` | Create/edit/delete cameras, start/stop recording    |

---

## React / hls.js Quick-Start

Install the library:
```bash
npm install hls.js
```

### Live stream component

```tsx
import Hls from 'hls.js';
import { useEffect, useRef } from 'react';

const BASE = '/feeder-service/api';

interface Props {
  cameraName: string;
  token: string;
}

export function LiveStream({ cameraName, token }: Props) {
  const videoRef = useRef<HTMLVideoElement>(null);

  useEffect(() => {
    const url = `${BASE}/camera/${cameraName}/live/playlist.m3u8`;
    const video = videoRef.current!;

    if (Hls.isSupported()) {
      const hls = new Hls({
        xhrSetup: (xhr) => {
          xhr.setRequestHeader('Authorization', `Bearer ${token}`);
        },
      });
      hls.loadSource(url);
      hls.attachMedia(video);
      return () => hls.destroy();
    } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
      // Safari native HLS — attach token via URL param if your server supports it
      video.src = url;
    }
  }, [cameraName, token]);

  return <video ref={videoRef} controls autoPlay muted style={{ width: '100%' }} />;
}
```

### VOD playback

```tsx
const vodUrl =
  `${BASE}/camera/${cameraName}/vod/playlist.m3u8` +
  `?date=2026-04-26&from=14:00&to=16:00`;

// Use the same hls.js setup above but with vodUrl instead.
```

### Fetch recording days

```ts
async function getRecordingDays(cameraName: string, token: string) {
  const res = await fetch(`${BASE}/camera/${cameraName}/recordings`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return res.json(); // RecordingDayDto[]
}
```

### Check recording status

```ts
async function getStatus(token: string) {
  const res = await fetch(`${BASE}/camera/status`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return res.json(); // { [cameraName: string]: boolean }
}
```

