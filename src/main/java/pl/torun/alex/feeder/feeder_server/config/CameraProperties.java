package pl.torun.alex.feeder.feeder_server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Typed configuration for all IP cameras.
 *
 * Each camera entry maps to one FFmpeg process that continuously writes
 * MPEG-TS segment files to its own storage directory.
 *
 * Adding a second camera in the future requires only a new
 * camera.cameras[N].* block in the active application properties file —
 * no code changes needed.
 */
@Configuration
@ConfigurationProperties(prefix = "camera")
@Data
public class CameraProperties {

    /** List of cameras. Index order does not matter. */
    private List<CameraConfig> cameras = new ArrayList<>();

    /**
     * Path to the FFmpeg executable. Use the plain name {@code ffmpeg} when it
     * is on the system PATH, or supply an absolute path (e.g.
     * {@code /usr/bin/ffmpeg}) when the PATH is not inherited by the JVM.
     */
    private String ffmpegPath = "ffmpeg";

    /** Length of each .ts segment in seconds. Default: 600 (10 min). */
    private int segmentDurationSeconds = 600;

    /** Maximum total disk space across ALL cameras combined, in bytes. Default: 100 GB. */
    private long maxStorageBytes = 107_374_182_400L;

    /** How often (ms) the rollover task checks disk usage. */
    private long rolloverCheckIntervalMs = 60_000L;

    /**
     * How often (ms) the service checks whether a camera's FFmpeg process is
     * still alive and, if not, attempts to reconnect and restart recording.
     * Applies only to cameras with {@code autoStart=true}.
     * Default: 30 seconds.
     */
    private long reconnectIntervalMs = 30_000L;

    @Data
    public static class CameraConfig {

        /** Unique logical name — used in file names and REST API paths. */
        private String name;

        /** Full RTSP URL including credentials. */
        private String rtspUrl;

        /** Absolute path to the directory where .ts segment files will be written. */
        private String storagePath;

        /**
         * When true the recording starts automatically on application startup
         * and is restarted automatically whenever the FFmpeg process dies.
         */
        private boolean autoStart = false;
    }
}


