package pl.torun.alex.feeder.feeder_server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Typed configuration for FFmpeg recording behaviour.
 * Camera instances (name, RTSP URL, storage path) are managed via the
 * {@code camera} DB table — see the {@code Camera} entity.
 */
@Configuration
@ConfigurationProperties(prefix = "camera")
@Data
public class CameraProperties {


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

    /**
     * Socket timeout for FFmpeg in microseconds (for the -stimeout parameter).
     * If no data is received from the camera for this duration, FFmpeg will exit.
     * Default: 10000000 (10 seconds).
     */
    private long ffmpegStimeoutUs = 10_000_000L;

    /**
     * Number of completed segments included in the live HLS playlist.
     * Each segment is {@code segmentDurationSeconds} long, so this controls
     * how far back a viewer can seek in the live stream.
     * Default: 5 segments = 50 minutes of live buffer.
     */
    private int liveWindowSegments = 5;

    /**
     * When {@code true} (default), FFmpeg transcodes video to H.264 (libx264)
     * and audio to AAC so that the resulting HLS segments are playable in any
     * browser via Media Source Extensions.
     *
     * <p>Set to {@code false} only if the camera already delivers an H.264 + AAC
     * stream and you want to skip the CPU cost of re-encoding.  Using copy-through
     * with an H.265 / HEVC source will cause {@code bufferAddCodecError} in hls.js.</p>
     */
    private boolean transcodeForBrowser = true;

    /**
     * CRF (Constant Rate Factor) value passed to libx264 when
     * {@code transcodeForBrowser=true}.  Lower = higher quality / larger files.
     * Default: 23 (libx264 default).
     */
    private int transcodeCrf = 23;
}
