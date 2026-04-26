package pl.torun.alex.feeder.feeder_server.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pl.torun.alex.feeder.feeder_server.dto.RecordingDayDto;
import pl.torun.alex.feeder.feeder_server.dto.SegmentInfoDto;
import pl.torun.alex.feeder.feeder_server.service.CameraService;
import pl.torun.alex.feeder.feeder_server.service.CameraStreamService;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * REST API for HLS video streaming and recording browsing.
 *
 * <pre>
 * ── Live stream ──────────────────────────────────────────────────────────────
 * GET /camera/{name}/live/playlist.m3u8
 *     Returns a sliding-window HLS EVENT playlist of the last N segments.
 *     hls.js should poll this URL to receive newly completed segments.
 *
 * ── VOD (historical playback) ────────────────────────────────────────────────
 * GET /camera/{name}/vod/playlist.m3u8?date=2026-04-26[&from=14:00&to=16:00]
 *     Returns a closed HLS VOD playlist for the given date.
 *     Optional from/to (HH:mm) filter the time window.
 *
 * ── Segment file serving ─────────────────────────────────────────────────────
 * GET /camera/{name}/segments/{filename}
 *     Streams a single .ts segment as video/MP2T.
 *     Supports Range requests so the player can seek inside a segment.
 *
 * ── Recording browser ────────────────────────────────────────────────────────
 * GET /camera/{name}/recordings
 *     Returns a list of days that have recordings, newest first.
 *
 * GET /camera/{name}/recordings/{date}
 *     Returns the individual segment list for a specific day.
 * </pre>
 *
 * All endpoints require JWT authentication.
 * Playlist and listing endpoints require {@code read-schedule} authority.
 */
@RestController
@RequestMapping("/camera")
@RequiredArgsConstructor
@Slf4j
public class CameraStreamController {

    /** MIME type for MPEG-TS segments. */
    private static final MediaType MEDIA_TYPE_MPEGTS =
            MediaType.parseMediaType("video/MP2T");

    /** MIME type for HLS playlists. */
    private static final MediaType MEDIA_TYPE_M3U8 =
            MediaType.parseMediaType("application/vnd.apple.mpegurl");

    private final CameraStreamService cameraStreamService;
    private final CameraService cameraService;

    // -------------------------------------------------------------------------
    // Live playlist
    // -------------------------------------------------------------------------

    /**
     * HLS EVENT playlist – sliding window of the last N completed segments.
     *
     * <p>hls.js usage (React):
     * <pre>
     * const hls = new Hls({ xhrSetup: xhr => xhr.setRequestHeader('Authorization', `Bearer ${token}`) });
     * hls.loadSource('/feeder-service/api/camera/CatCamMaster/live/playlist.m3u8');
     * hls.attachMedia(videoRef.current);
     * </pre>
     */
    @GetMapping("/{name}/live/playlist.m3u8")
    @PreAuthorize("hasAuthority('read-schedule')")
    public ResponseEntity<String> livePlaylist(
            @PathVariable String name,
            @AuthenticationPrincipal String username,
            HttpServletRequest request) {

        if (!cameraService.hasAccess(name, username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String playlist = cameraStreamService.generateLivePlaylist(name, contextPath(request));
        return ResponseEntity.ok()
                .contentType(MEDIA_TYPE_M3U8)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store")
                .body(playlist);
    }

    // -------------------------------------------------------------------------
    // VOD playlist
    // -------------------------------------------------------------------------

    /**
     * HLS VOD playlist for a specific date with an optional time-window filter.
     *
     * @param date ISO date, e.g. {@code 2026-04-26}
     * @param from optional inclusive start time, e.g. {@code 14:00}
     * @param to   optional exclusive end   time, e.g. {@code 16:00}
     */
    @GetMapping("/{name}/vod/playlist.m3u8")
    @PreAuthorize("hasAuthority('read-schedule')")
    public ResponseEntity<String> vodPlaylist(
            @PathVariable String name,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime to,
            @AuthenticationPrincipal String username,
            HttpServletRequest request) {

        if (!cameraService.hasAccess(name, username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String playlist = cameraStreamService.generateVodPlaylist(
                name, date, from, to, contextPath(request));

        return ResponseEntity.ok()
                .contentType(MEDIA_TYPE_M3U8)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=60")
                .body(playlist);
    }

    // -------------------------------------------------------------------------
    // Segment serving
    // -------------------------------------------------------------------------

    /**
     * Streams a single .ts segment file.
     *
     * <p>Spring's {@link org.springframework.web.servlet.resource.ResourceHttpRequestHandler}
     * is not used here so that we can enforce per-camera path isolation and JWT
     * authentication on every segment request.  Range requests (for mid-segment
     * seeking) are handled automatically by Spring when returning a
     * {@link FileSystemResource}.</p>
     *
     * @param name     camera name
     * @param filename segment filename, e.g. {@code CatCamMaster_2026-04-26_14-30-00.ts}
     */
    @GetMapping("/{name}/segments/{filename}")
    @PreAuthorize("hasAuthority('read-schedule')")
    public ResponseEntity<Resource> segment(
            @PathVariable String name,
            @PathVariable String filename,
            @AuthenticationPrincipal String username) {

        if (!cameraService.hasAccess(name, username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Path segmentPath = cameraStreamService.resolveSegment(name, filename);
        Resource resource = new FileSystemResource(segmentPath);

        return ResponseEntity.ok()
                .contentType(MEDIA_TYPE_MPEGTS)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .body(resource);
    }

    // -------------------------------------------------------------------------
    // Recording browser
    // -------------------------------------------------------------------------

    /**
     * Lists all days that have at least one recording for the given camera,
     * sorted newest first.  Each entry includes segment count, total size,
     * and the individual segment list.
     */
    @GetMapping("/{name}/recordings")
    @PreAuthorize("hasAuthority('read-schedule')")
    public ResponseEntity<List<RecordingDayDto>> recordingDays(
            @PathVariable String name,
            @AuthenticationPrincipal String username) {
        if (!cameraService.hasAccess(name, username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(cameraStreamService.listRecordingDays(name));
    }

    @GetMapping("/{name}/recordings/{date}")
    @PreAuthorize("hasAuthority('read-schedule')")
    public ResponseEntity<List<SegmentInfoDto>> recordingDay(
            @PathVariable String name,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal String username) {
        if (!cameraService.hasAccess(name, username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(cameraStreamService.listSegmentsForDay(name, date));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Extracts the servlet context path from the request so that segment URLs
     * embedded in playlists are correct regardless of how the app is deployed.
     * e.g. returns {@code /feeder-service/api}.
     */
    private String contextPath(HttpServletRequest request) {
        return request.getContextPath();
    }
}

