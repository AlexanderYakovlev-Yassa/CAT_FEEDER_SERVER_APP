package pl.torun.alex.feeder.feeder_server.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.torun.alex.feeder.feeder_server.config.CameraProperties;
import pl.torun.alex.feeder.feeder_server.entity.Camera;
import pl.torun.alex.feeder.feeder_server.repository.CameraRepository;
import pl.torun.alex.feeder.feeder_server.dto.RecordingDayDto;
import pl.torun.alex.feeder.feeder_server.dto.SegmentInfoDto;
import pl.torun.alex.feeder.feeder_server.service.CameraStreamService;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Builds HLS playlists on-the-fly from the .ts segment files written by FFmpeg
 * and provides directory-listing helpers for the REST API.
 *
 * <h3>Filename convention</h3>
 * {@code {cameraName}_{yyyy-MM-dd}_{HH-mm-ss}.ts}
 * e.g. {@code CatCamMaster_2026-04-26_14-30-00.ts}
 *
 * <h3>Live playlist</h3>
 * Returns the last {@code camera.live-window-segments} segments as an
 * HLS event-type playlist.  The client (hls.js) polls this endpoint
 * periodically to pick up newly completed segments.
 *
 * <h3>VOD playlist</h3>
 * Returns all segments for a given date, optionally filtered by a time
 * window, as a closed HLS VOD playlist ({@code #EXT-X-ENDLIST}).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CameraStreamServiceImpl implements CameraStreamService {

    private static final DateTimeFormatter SEGMENT_DT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    /**
     * Matches filenames like {@code CatCamMaster_2026-04-26_14-30-00.ts}.
     * Group 1 = camera name, Group 2 = datetime string.
     */
    private static final Pattern SEGMENT_FILENAME_PATTERN =
            Pattern.compile("^(.+)_(\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2})\\.ts$");

    private final CameraProperties properties;
    private final CameraRepository cameraRepository;
    private final pl.torun.alex.feeder.feeder_server.mqtt.MqttService mqttService;

    // -------------------------------------------------------------------------
    // HLS playlists
    // -------------------------------------------------------------------------

    @Override
    public String generateLivePlaylist(String cameraName, String baseApiPath) {
        List<SegmentInfoDto> all = listSegmentsSorted(cameraName);

        // Exclude the very last file – it is still being written by FFmpeg.
        // We only expose it once a newer segment has started (i.e. it is no
        // longer the most-recently-modified file).
        List<SegmentInfoDto> completed = all.size() > 1
                ? all.subList(0, all.size() - 1)
                : all;

        int window = properties.getLiveWindowSegments();
        List<SegmentInfoDto> windowSegments = completed.size() <= window
                ? completed
                : completed.subList(completed.size() - window, completed.size());

        // Sequence number = position of the first segment in the full list
        int mediaSequence = Math.max(0, completed.size() - window);

        return buildPlaylist(cameraName, baseApiPath, windowSegments, false, mediaSequence);
    }

    @Override
    public String generateVodPlaylist(String cameraName, LocalDate date,
                                      LocalTime from, LocalTime to,
                                      String baseApiPath) {
        List<SegmentInfoDto> segments = listSegmentsForDay(cameraName, date).stream()
                .filter(s -> from == null || !s.getStartTime().isBefore(from))
                .filter(s -> to   == null || s.getStartTime().isBefore(to))
                .collect(Collectors.toList());

        return buildPlaylist(cameraName, baseApiPath, segments, true, 0);
    }

    // -------------------------------------------------------------------------
    // Segment file resolution (with path-traversal guard)
    // -------------------------------------------------------------------------

    @Override
    public Path resolveSegment(String cameraName, String filename) {
        if (filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            throw new IllegalArgumentException("Invalid segment filename: " + filename);
        }
        if (!SEGMENT_FILENAME_PATTERN.matcher(filename).matches()) {
            throw new IllegalArgumentException("Filename does not match expected segment pattern: " + filename);
        }

        Camera cam = findCamera(cameraName);
        Path storageDir = Path.of(cam.getStoragePath()).toAbsolutePath().normalize();
        Path segmentPath = storageDir.resolve(filename).normalize();

        // Ensure the resolved path is still inside the camera's storage directory.
        if (!segmentPath.startsWith(storageDir)) {
            throw new IllegalArgumentException("Segment path escapes storage directory: " + filename);
        }
        if (!Files.exists(segmentPath)) {
            throw new IllegalArgumentException("Segment not found: " + filename);
        }
        return segmentPath;
    }

    // -------------------------------------------------------------------------
    // Listing helpers
    // -------------------------------------------------------------------------

    @Override
    public List<RecordingDayDto> listRecordingDays(String cameraName) {
        List<SegmentInfoDto> allSegments = listSegmentsSorted(cameraName);

        Map<LocalDate, List<SegmentInfoDto>> byDay = allSegments.stream()
                .collect(Collectors.groupingBy(SegmentInfoDto::getDate));

        return byDay.entrySet().stream()
                .sorted(Map.Entry.<LocalDate, List<SegmentInfoDto>>comparingByKey().reversed())
                .map(entry -> {
                    List<SegmentInfoDto> segs = entry.getValue();
                    long total = segs.stream().mapToLong(SegmentInfoDto::getSizeBytes).sum();
                    return RecordingDayDto.builder()
                            .date(entry.getKey())
                            .segmentCount(segs.size())
                            .totalSizeBytes(total)
                            .segments(segs)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<SegmentInfoDto> listSegmentsForDay(String cameraName, LocalDate date) {
        return listSegmentsSorted(cameraName).stream()
                .filter(s -> date.equals(s.getDate()))
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Lists and parses all valid .ts segment files for the camera,
     * sorted ascending by their embedded start time.
     */
    private List<SegmentInfoDto> listSegmentsSorted(String cameraName) {
        Camera cam = findCamera(cameraName);
        Path storageDir = Path.of(cam.getStoragePath());

        if (!Files.exists(storageDir)) {
            return List.of();
        }

        try (Stream<Path> files = Files.list(storageDir)) {
            return files
                    .filter(p -> p.getFileName().toString().endsWith(".ts"))
                    .flatMap(p -> parseSegment(p).stream())
                    .sorted(Comparator.comparing(s -> LocalDateTime.of(s.getDate(), s.getStartTime())))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.warn("Cannot list segments for camera '{}': {}", cameraName, e.getMessage());
            return List.of();
        }
    }

    /**
     * Attempts to parse a segment file into a {@link SegmentInfoDto}.
     * Returns an empty list (flatMap-friendly) if the filename doesn't match
     * the expected pattern or the file can't be stat'd.
     */
    private List<SegmentInfoDto> parseSegment(Path file) {
        String filename = file.getFileName().toString();
        Matcher m = SEGMENT_FILENAME_PATTERN.matcher(filename);
        if (!m.matches()) return List.of();

        try {
            LocalDateTime dt = LocalDateTime.parse(m.group(2), SEGMENT_DT_FORMATTER);
            long size = Files.size(file);
            return List.of(SegmentInfoDto.builder()
                    .filename(filename)
                    .date(dt.toLocalDate())
                    .startTime(dt.toLocalTime())
                    .sizeBytes(size)
                    .build());
        } catch (Exception e) {
            log.debug("Could not parse segment file '{}': {}", filename, e.getMessage());
            return List.of();
        }
    }

    /**
     * Builds an HLS playlist string.
     *
     * @param vod            true  → add {@code #EXT-X-ENDLIST} (VOD)
     *                       false → omit it (live / event stream)
     * @param mediaSequence  value for {@code #EXT-X-MEDIA-SEQUENCE}
     */
    private String buildPlaylist(String cameraName, String baseApiPath,
                                 List<SegmentInfoDto> segments,
                                 boolean vod, int mediaSequence) {
        int targetDuration = properties.getSegmentDurationSeconds();

        StringBuilder sb = new StringBuilder();
        sb.append("#EXTM3U\n");
        sb.append("#EXT-X-VERSION:3\n");
        sb.append("#EXT-X-TARGETDURATION:").append(targetDuration).append("\n");
        sb.append("#EXT-X-MEDIA-SEQUENCE:").append(mediaSequence).append("\n");

        if (vod) {
            sb.append("#EXT-X-PLAYLIST-TYPE:VOD\n");
        } else {
            // EVENT type: player polls the playlist URL for new segments.
            sb.append("#EXT-X-PLAYLIST-TYPE:EVENT\n");
            sb.append("#EXT-X-ALLOW-CACHE:NO\n");
        }

        sb.append("\n");

        for (SegmentInfoDto seg : segments) {
            sb.append("#EXTINF:").append(targetDuration).append(".0,\n");
            sb.append(baseApiPath)
              .append("/camera/").append(cameraName)
              .append("/segments/").append(seg.getFilename())
              .append("\n");
        }

        if (vod) {
            sb.append("#EXT-X-ENDLIST\n");
        }

        return sb.toString();
    }

    private Camera findCamera(String name) {
        return cameraRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Unknown camera: " + name));
    }

    private static final Pattern RTSP_URL_PATTERN =
            Pattern.compile("rtsp://(?:([^:]+):([^@]+)@)?([^:/]+)(?::(\\d+))?.*");

    @Override
    public byte[] captureSnapshot(String cameraName) {
        Camera cam = findCamera(cameraName);
        String rtspUrl = cam.getRtspUrl();

        // 1. Сначала пробуем прямой HTTP-снимок с ONVIF порта 8899 (мгновенно, без создания лишних RTSP-сессий)
        byte[] httpSnap = tryHttpSnapshot(rtspUrl);
        if (httpSnap != null && httpSnap.length > 0) {
            log.debug("Captured camera snapshot for '{}' via direct HTTP", cameraName);
            return httpSnap;
        }

        // 2. Fallback: FFmpeg по субпотоку RTSP
        String snapshotRtsp = rtspUrl.contains("/live/ch0") ? rtspUrl.replace("/live/ch0", "/live/ch1") : rtspUrl;

        List<String> command = List.of(
                properties.getFfmpegPath(),
                "-nostdin",
                "-y",
                "-loglevel", "error",
                "-rtsp_transport", "tcp",
                "-timeout", "10000000",
                "-i", snapshotRtsp,
                "-vframes", "1",
                "-f", "image2pipe",
                "-vcodec", "mjpeg",
                "-"
        );

        try {
            Process process = new ProcessBuilder(command).start();
            byte[] bytes = process.getInputStream().readAllBytes();
            boolean finished = process.waitFor(8, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("FFmpeg snapshot timed out");
            }
            if (bytes.length == 0) {
                throw new IllegalStateException("FFmpeg captured empty snapshot");
            }
            return bytes;
        } catch (Exception e) {
            log.error("Failed to capture camera snapshot for '{}': {}", cameraName, e.getMessage());
            throw new RuntimeException("Snapshot capture failed: " + e.getMessage(), e);
        }
    }

    private byte[] tryHttpSnapshot(String rtspUrl) {
        if (rtspUrl == null || rtspUrl.isBlank()) return null;
        try {
            Matcher m = RTSP_URL_PATTERN.matcher(rtspUrl);
            if (!m.matches()) return null;

            String user = m.group(1);
            String pass = m.group(2);
            String host = m.group(3);

            // Cinnado / Tuya IP-камеры отдают мгновенный JPEG по порту 8899
            URI uri = URI.create("http://" + host + ":8899/api/v1/snap.cgi?chn=1");
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build();

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(3))
                    .GET();

            if (user != null && pass != null) {
                String auth = Base64.getEncoder().encodeToString(
                        (user + ":" + pass).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                reqBuilder.header("Authorization", "Basic " + auth);
            }

            HttpResponse<byte[]> response = client.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 200 && response.body() != null && response.body().length > 100) {
                return response.body();
            }
        } catch (Exception e) {
            log.debug("Direct HTTP snapshot attempt failed: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public String getDetectionZones(String cameraName) {
        Camera cam = findCamera(cameraName);
        return cam.getDetectionZones() != null ? cam.getDetectionZones() : "[]";
    }

    @Override
    public void updateDetectionZones(String cameraName, String zonesJson) {
        Camera cam = findCamera(cameraName);
        cam.setDetectionZones(zonesJson);
        cameraRepository.save(cam);

        try {
            String topic = "catfeeder/camera/zones/update";
            String payload = "{\"camera\":\"" + cameraName + "\",\"zones\":" + zonesJson + "}";
            mqttService.publish(topic, payload, 1, false);
            log.info("Published updated detection zones for camera '{}' to MQTT topic '{}'", cameraName, topic);
        } catch (Exception e) {
            log.warn("Failed to publish updated zones to MQTT: {}", e.getMessage());
        }
    }
}

