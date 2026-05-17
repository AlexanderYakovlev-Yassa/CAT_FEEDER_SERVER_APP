package pl.torun.alex.feeder.feeder_server.service.impl;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pl.torun.alex.feeder.feeder_server.config.CameraProperties;
import pl.torun.alex.feeder.feeder_server.entity.Camera;
import pl.torun.alex.feeder.feeder_server.repository.CameraRepository;
import pl.torun.alex.feeder.feeder_server.service.CameraRecordingService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages per-camera FFmpeg recording processes.
 *
 * <p>Each camera runs its own {@code ffmpeg} process using the segment muxer,
 * which automatically splits the RTSP stream into fixed-length MPEG-TS files.
 * By default ({@code camera.transcode-for-browser=true}) the stream is
 * re-encoded to <b>H.264 video + AAC audio</b> so that hls.js can play the
 * segments in any browser via Media Source Extensions.  Set
 * {@code camera.transcode-for-browser=false} only when the camera already
 * delivers an H.264 + AAC stream and you want to skip the CPU cost of
 * re-encoding.
 * File names include the camera name, date and start time, e.g.:
 * {@code CatCamMaster_2026-04-26_14-30-00.ts}</p>
 *
 * <p><b>Rollover:</b> a scheduled task runs every minute, sums the total size
 * of all .ts files across all camera directories, and deletes the oldest files
 * until usage falls below {@code camera.max-storage-bytes} (default 100 GB).</p>
 *
 * <p><b>Auto-resume:</b> on startup (and every {@code camera.reconnect-interval-ms})
 * any {@code autoStart=true} camera whose FFmpeg process has died is
 * automatically restarted.</p>
 *
 * <p><b>FFmpeg binary:</b> the binary path is configured via {@code camera.ffmpeg-path}
 * (default {@code ffmpeg}). If the binary cannot be found the service starts normally
 * but logs a single clear warning; the reconnect watchdog suppresses repeated error
 * spam until the binary becomes available.</p>
 *
 * <p><b>FFmpeg logs:</b> each camera writes its FFmpeg stdout/stderr to
 * {@code <storagePath>/<cameraName>_ffmpeg.log} for troubleshooting.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CameraRecordingServiceImpl implements CameraRecordingService {

    private final CameraProperties properties;
    private final CameraRepository cameraRepository;

    /** Active FFmpeg process keyed by camera name. */
    private final Map<String, Process> activeProcesses = new ConcurrentHashMap<>();

    /**
     * Cameras whose last start attempt failed because the FFmpeg binary was not
     * found. These are skipped silently in the reconnect watchdog to avoid log
     * spam. The flag is cleared whenever {@link #startRecording} is called
     * explicitly via the REST API so a manual retry is always attempted.
     */
    private final Set<String> binaryMissingCameras = ConcurrentHashMap.newKeySet();

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @PostConstruct
    public void init() {
        log.info("Initializing camera recording service (ffmpeg binary: '{}')…",
                properties.getFfmpegPath());

        if (!isFfmpegAvailable()) {
            log.warn("FFmpeg binary '{}' not found on this host. " +
                            "Camera recording is disabled until FFmpeg is installed and the " +
                            "application is restarted or recording is started via the REST API.",
                    properties.getFfmpegPath());
        }

        cameraRepository.findByAutoStartTrue().forEach(cam -> {
                    try {
                        startRecording(cam.getName());
                    } catch (Exception e) {
                        // A single camera failure must never prevent the whole application from starting.
                        log.error("Could not start recording for camera '{}' on init: {}",
                                cam.getName(), e.getMessage());
                    }
                });
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down all camera recording processes…");
        activeProcesses.forEach((name, process) -> {
            if (process.isAlive()) {
                process.destroy();
                log.info("Stopped FFmpeg for camera '{}'", name);
            }
        });
        activeProcesses.clear();
    }

    // -------------------------------------------------------------------------
    // CameraRecordingService
    // -------------------------------------------------------------------------

    @Override
    public synchronized void startRecording(String cameraName) {
        // Explicit API call: always clear the "binary missing" flag and retry.
        binaryMissingCameras.remove(cameraName);

        Process existing = activeProcesses.get(cameraName);
        if (existing != null && existing.isAlive()) {
            log.info("Recording already active for camera '{}'", cameraName);
            return;
        }

        Camera cam = findCamera(cameraName);

        try {
            Path storageDir = Path.of(cam.getStoragePath());
            Files.createDirectories(storageDir);

            // Output pattern: <storagePath>/<cameraName>_<date>_<time>.ts
            // -strftime 1 tells FFmpeg to expand %Y, %m, %d, %H, %M, %S at the moment
            // each new segment file is opened, so every file gets its actual start time.
            String outputPattern = storageDir
                    .resolve(cam.getName() + "_%Y-%m-%d_%H-%M-%S.ts")
                    .toString();

            // Per-camera FFmpeg log so stdout/stderr are never silently lost.
            Path ffmpegLog = storageDir.resolve(cam.getName() + "_ffmpeg.log");

            // Use the RTSP-specific `-timeout` parameter (in seconds) as it's more compatible
            // than the general `-stimeout` (in microseconds).
            long timeoutSeconds = properties.getFfmpegStimeoutUs() / 1_000_000;

            // Build the codec arguments.
            // transcodeForBrowser=true (default): re-encode to H.264 + AAC so that
            // hls.js can play the segments in every browser via Media Source Extensions.
            // transcodeForBrowser=false: pass-through copy – only safe when the camera
            // already delivers H.264 + AAC; using copy with H.265/HEVC will produce
            // bufferAddCodecError in the frontend player.
            List<String> codecArgs;
            if (properties.isTranscodeForBrowser()) {
                codecArgs = List.of(
                        "-c:v", "libx264",
                        "-profile:v", "main",
                        "-pix_fmt", "yuv420p",
                        "-crf", String.valueOf(properties.getTranscodeCrf()),
                        "-preset", "veryfast",
                        "-c:a", "aac"
                );
                log.debug("Using H.264+AAC transcode for camera '{}'", cameraName);
            } else {
                codecArgs = List.of("-c", "copy");
                log.debug("Using copy-through for camera '{}' (transcodeForBrowser=false)", cameraName);
            }

            List<String> command = new java.util.ArrayList<>(List.of(
                    properties.getFfmpegPath(),
                    "-y",                          // overwrite output files without prompting
                    "-rtsp_transport", "tcp",       // TCP is more reliable for home cams
                    "-timeout", String.valueOf(timeoutSeconds),
                    "-i", cam.getRtspUrl()
            ));
            command.addAll(codecArgs);
            command.addAll(List.of(
                    "-f", "segment",
                    "-segment_time", String.valueOf(properties.getSegmentDurationSeconds()),
                    "-segment_format", "mpegts",
                    "-strftime", "1",              // enable time format substitution in filename
                    "-reset_timestamps", "1",      // start each segment's timestamps from 0
                    outputPattern
            ));

            log.debug("FFmpeg command: {}", String.join(" ", command));

            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .redirectOutput(ffmpegLog.toFile())
                    .start();

            activeProcesses.put(cameraName, process);
            log.info("Started recording for camera '{}' (PID {}), segments: {}",
                    cameraName, process.pid(), outputPattern);

        } catch (IOException e) {
            if (isBinaryMissingError(e)) {
                binaryMissingCameras.add(cameraName);
                log.error("FFmpeg binary '{}' not found – cannot record camera '{}'. " +
                                "Install FFmpeg or set camera.ffmpeg-path to the correct location. " +
                                "Further reconnect attempts for this camera will be suppressed until " +
                                "the binary is available.",
                        properties.getFfmpegPath(), cameraName);
            } else {
                log.error("Failed to start FFmpeg for camera '{}': {}", cameraName, e.getMessage(), e);
            }
            throw new RuntimeException("Could not start FFmpeg for camera: " + cameraName, e);
        }
    }

    @Override
    public synchronized void stopRecording(String cameraName) {
        Process process = activeProcesses.remove(cameraName);
        if (process == null || !process.isAlive()) {
            log.info("No active recording found for camera '{}'", cameraName);
            return;
        }
        process.destroy();
        log.info("Stopped recording for camera '{}'", cameraName);
    }

    @Override
    public Map<String, Boolean> getRecordingStatus() {
        return cameraRepository.findAll().stream()
                .collect(Collectors.toMap(
                        Camera::getName,
                        cam -> {
                            Process p = activeProcesses.get(cam.getName());
                            return p != null && p.isAlive();
                        }
                ));
    }

    // -------------------------------------------------------------------------
    // Scheduled tasks
    // -------------------------------------------------------------------------

    /**
     * Enforces the disk-space ceiling.
     * Collects all .ts files from every camera directory, and if the total
     * exceeds {@code camera.max-storage-bytes}, deletes the oldest files first
     * until usage is back within the limit.
     */
    @Scheduled(fixedDelayString = "${camera.rollover-check-interval-ms:60000}")
    public void performRolloverCheck() {
        List<Path> allSegments = collectAllSegments();

        long totalBytes = allSegments.stream()
                .mapToLong(this::fileSizeQuiet)
                .sum();

        if (totalBytes <= properties.getMaxStorageBytes()) {
            return;
        }

        log.info("Storage usage {} MB exceeds limit {} MB – removing oldest segments…",
                toMb(totalBytes), toMb(properties.getMaxStorageBytes()));

        // Sort oldest-first by last-modified time
        allSegments.sort(Comparator.comparingLong(this::lastModifiedQuiet));

        for (Path file : allSegments) {
            if (totalBytes <= properties.getMaxStorageBytes()) break;
            try {
                long size = Files.size(file);
                Files.delete(file);
                totalBytes -= size;
                log.info("Deleted old segment: {}", file.getFileName());
            } catch (IOException e) {
                log.warn("Could not delete segment {}: {}", file, e.getMessage());
            }
        }
    }

    /**
     * Reconnect watchdog: runs every {@code camera.reconnect-interval-ms} (default 30 s).
     * For every {@code autoStart=true} camera whose FFmpeg process is no longer alive
     * (camera reboot, network dropout, etc.) a fresh FFmpeg process is started.
     *
     * <p>Cameras blocked by a missing FFmpeg binary are skipped silently to avoid
     * log spam — the problem was already reported once when the failure first occurred.
     * A manual call to {@code POST /camera/{name}/start} always bypasses this guard.</p>
     */
    @Scheduled(fixedDelayString = "${camera.reconnect-interval-ms:30000}")
    public synchronized void checkAndRestartDeadProcesses() {
        // Re-check whether the binary has become available since last tick.
        if (!binaryMissingCameras.isEmpty() && isFfmpegAvailable()) {
            log.info("FFmpeg binary '{}' is now available – resuming reconnect attempts for: {}",
                    properties.getFfmpegPath(), binaryMissingCameras);
            binaryMissingCameras.clear();
        }

        cameraRepository.findByAutoStartTrue().forEach(cam -> {
            if (binaryMissingCameras.contains(cam.getName())) {
                log.debug("Skipping reconnect for camera '{}' – FFmpeg binary not available.",
                        cam.getName());
                return;
            }

            Process p = activeProcesses.get(cam.getName());
            if (p == null || !p.isAlive()) {
                log.warn("FFmpeg process for camera '{}' is not running – reconnecting…",
                        cam.getName());
                activeProcesses.remove(cam.getName());
                try {
                    startRecording(cam.getName());
                } catch (Exception e) {
                    log.warn("Reconnect attempt for camera '{}' failed – will retry in {} ms.",
                            cam.getName(), properties.getReconnectIntervalMs());
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Returns true if the configured FFmpeg binary can be found on PATH or as
     * an absolute file.  Uses a cheap {@code --version} probe.
     */
    private boolean isFfmpegAvailable() {
        try {
            new ProcessBuilder(properties.getFfmpegPath(), "-version")
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start()
                    .waitFor();
            return true;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    /**
     * Returns true when the IOException indicates that the executable itself
     * could not be found (as opposed to a connectivity or permission error).
     */
    private boolean isBinaryMissingError(IOException e) {
        String msg = e.getMessage();
        return msg != null && (msg.contains("No such file or directory") || msg.contains("error=2"));
    }

    private Camera findCamera(String name) {
        return cameraRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Unknown camera: " + name));
    }

    private List<Path> collectAllSegments() {
        return cameraRepository.findAll().stream()
                .flatMap(cam -> {
                    Path dir = Path.of(cam.getStoragePath());
                    if (!Files.exists(dir)) return Stream.empty();
                    try (Stream<Path> files = Files.list(dir)) {
                        return files
                                .filter(p -> p.toString().endsWith(".ts"))
                                .toList()
                                .stream();
                    } catch (IOException e) {
                        log.warn("Cannot list files in {}: {}", dir, e.getMessage());
                        return Stream.empty();
                    }
                })
                .collect(Collectors.toList());
    }

    private long fileSizeQuiet(Path p) {
        try { return Files.size(p); } catch (IOException e) { return 0L; }
    }

    private long lastModifiedQuiet(Path p) {
        try { return Files.getLastModifiedTime(p).toMillis(); } catch (IOException e) { return Long.MAX_VALUE; }
    }

    private long toMb(long bytes) {
        return bytes / (1024 * 1024);
    }
}