# Camera Playback Troubleshooting — Backend Developer Guide

Base URL: `/feeder-service/api`

This note explains a frontend playback failure observed in the camera tab and the backend-side fixes that usually resolve it.

---

## Observed Failure

The frontend HLS player reported these errors for the live stream endpoint:

- `type: mediaError`
- `details: bufferAddCodecError`
- `details: bufferAppendError`

Example stream URL:

```text
/feeder-service/api/camera/cat%20cam%20master/live/playlist.m3u8
```

These errors occur after the playlist is loaded and hls.js starts building browser `MediaSource` buffers.

---

## What This Usually Means

The browser reached the stream endpoint, but the returned HLS media is not compatible with Media Source Extensions playback.

The strongest signal is `bufferAddCodecError`.

That usually means one of these conditions is true:

1. The stream codec is not supported by the browser.
2. The HLS playlist advertises codecs incorrectly.
3. The segment data is malformed, truncated, or inconsistent with the playlist metadata.
4. The stream contains track metadata that cannot be mapped into a valid browser `SourceBuffer`.

The follow-up `bufferAppendError` is often a secondary failure after buffer creation or codec negotiation already failed.

---

## Most Likely Root Cause

The most likely root cause is that the backend is exposing camera media in a codec/container combination that works for RTSP or FFmpeg pipelines but not for browser HLS playback.

Common examples:

- H.265 / HEVC video instead of H.264 / AVC
- Unsupported audio codec instead of AAC
- `copy`-through from the RTSP source without browser-safe transcoding
- Playlist `CODECS` metadata that does not match the actual segment payload

For browser playback, the safest target is:

- Video: H.264 / AVC
- Audio: AAC
- Pixel format: `yuv420p`
- HLS playlist with valid segment metadata and browser-playable MIME behavior

---

## Backend Fix Options

### Option 1: Transcode to browser-safe HLS

If the camera provides HEVC or another incompatible codec, do not pass it through directly to HLS.

Preferred approach:

- Transcode video to H.264
- Transcode audio to AAC
- Produce clean HLS output for both live and VOD playlists

Example FFmpeg direction:

```bash
ffmpeg -i rtsp://... \
  -c:v libx264 \
  -profile:v main \
  -pix_fmt yuv420p \
  -c:a aac \
  -f hls ...
```

Notes:

- Avoid `-c:v copy` unless the incoming codec is already browser-safe.
- If there is no usable audio track, handle that explicitly instead of emitting invalid audio metadata.
- Keep the generated HLS variant simple until playback is stable.

### Option 2: Fix playlist codec metadata

If the backend generates `#EXT-X-STREAM-INF` or codec descriptors, ensure the `CODECS` value matches the actual encoded media.

Problems to avoid:

- Manifest declares H.264 while segments contain HEVC
- Manifest declares AAC while segments contain unsupported audio
- Missing or malformed codec metadata in master playlists

### Option 3: Validate segment integrity

If the codec is correct but append still fails, inspect the first segments referenced by the playlist.

Check for:

- Empty or truncated `.ts` files
- Invalid PAT/PMT or transport stream structure
- Broken timestamps or discontinuities
- HTML or JSON error responses being returned as segment content
- Segment paths in the playlist pointing to the wrong files

### Option 4: Simplify the live pipeline

For live playback, reduce complexity until the stream is stable.

Try:

- Single video stream
- AAC audio only, or no audio if audio is not needed
- Short HLS segment duration with consistent GOP structure
- No exotic codecs, filters, or container flags

### Option 5: Add backend-side validation before publishing playlists

Before serving a live or VOD playlist, validate that:

- The playlist starts with `#EXTM3U`
- Referenced segments exist
- The first segment is readable
- The generated codec combination is browser-playable
- The playlist does not reference stale or deleted files

This reduces cases where the frontend receives a formally reachable but unusable stream.

---

## What To Check First

### 1. Inspect the playlist contents

Open the live or VOD playlist response and verify:

- It starts with `#EXTM3U`
- It contains valid HLS tags
- Segment URLs are correct
- If a master playlist is used, codec declarations are accurate

### 2. Inspect the actual segment codecs

Use `ffprobe` on one or more referenced segments:

```bash
ffprobe -hide_banner -show_streams segment.ts
```

Confirm:

- Video codec is `h264`
- Audio codec is `aac` if audio is present
- Resolution, profile, and pixel format are browser-safe

### 3. Compare manifest codecs to segment codecs

If the playlist declares codecs, verify they match the real media streams.

### 4. Check first-segment health

Browser playback often fails immediately if the first playable segment is malformed.

Validate:

- Non-zero file size
- Expected transport stream structure
- Decodable by FFmpeg or ffplay

### 5. Verify live output stability

For live HLS, ensure the backend is not serving incomplete segments or racing file creation.

---

## API Endpoints Involved

The frontend uses these endpoints for playback:

### Live HLS

```text
GET /camera/{name}/live/playlist.m3u8
```

### VOD HLS

```text
GET /camera/{name}/vod/playlist.m3u8?date=yyyy-MM-dd[&from=HH:mm&to=HH:mm]
```

### Segment Serving

```text
GET /camera/{name}/segments/{filename}
```

A response can be authorized and still fail playback if the media content itself is not browser-compatible.

---

## Recommended Backend Contract

For the frontend player to work reliably:

1. Playlist responses should be valid HLS manifests.
2. Segment URLs should resolve to actual MPEG-TS segment data.
3. Codecs should be browser-compatible, preferably H.264 + AAC.
4. Playlist metadata should match the actual encoded media.
5. Live playlists should not reference partial, stale, or broken segments.

---

## Practical Remediation Order

Use this order to converge quickly:

1. Run `ffprobe` on the first returned segment.
2. If the video codec is not `h264`, transcode to H.264.
3. If the audio codec is not `aac`, transcode audio or remove audio cleanly.
4. Verify playlist `CODECS` metadata matches the actual streams.
5. Re-test with a minimal HLS output configuration.
6. Only after playback works, reintroduce additional pipeline complexity.

---

## Minimal Acceptance Checklist

The backend fix is likely correct if all of the following are true:

- The frontend no longer logs `bufferAddCodecError`
- The frontend no longer logs `bufferAppendError`
- The playlist loads successfully
- The first segment appends successfully in the browser
- Video starts rendering in the live player
- The same encoding approach also works for VOD playback

---

## Summary

This failure does not primarily indicate a frontend bug.

The frontend reached the stream and tried to initialize playback, but the media pipeline failed at the browser buffer layer. The backend should first verify codec compatibility and segment correctness, then align playlist metadata with the actual encoded output.
