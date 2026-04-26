package pl.torun.alex.feeder.feeder_server.service;

import pl.torun.alex.feeder.feeder_server.dto.RecordingDayDto;
import pl.torun.alex.feeder.feeder_server.dto.SegmentInfoDto;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface CameraStreamService {

    /**
     * Generates an HLS live playlist (sliding window of the last
     * {@code camera.live-window-segments} completed segments).
     * The playlist is marked as an event stream so HLS.js polls for updates.
     *
     * @param cameraName logical camera name (must match configuration)
     * @param baseApiPath full context-path prefix for segment URLs,
     *                    e.g. {@code /feeder-service/api}
     */
    String generateLivePlaylist(String cameraName, String baseApiPath);

    /**
     * Generates an HLS VOD playlist containing every segment recorded on
     * {@code date}, optionally filtered to the half-open interval
     * [{@code from}, {@code to}).  Pass {@code null} for either bound to
     * include from the start / until the end of the day.
     *
     * @param cameraName  logical camera name
     * @param date        recording date (required)
     * @param from        optional inclusive start time filter
     * @param to          optional exclusive end time filter
     * @param baseApiPath full context-path prefix for segment URLs
     */
    String generateVodPlaylist(String cameraName, LocalDate date,
                               LocalTime from, LocalTime to,
                               String baseApiPath);

    /**
     * Resolves a segment filename to its absolute {@link Path} on disk.
     * Validates that the filename is safe (no path traversal) and that the
     * file actually belongs to the given camera's storage directory.
     *
     * @throws IllegalArgumentException if the filename is unsafe or unknown
     */
    Path resolveSegment(String cameraName, String filename);

    /**
     * Returns a summary of every day that has at least one recorded segment,
     * sorted in descending chronological order (newest first).
     */
    List<RecordingDayDto> listRecordingDays(String cameraName);

    /**
     * Returns ordered segment details for a single day.
     *
     * @return empty list if no recordings exist for that date
     */
    List<SegmentInfoDto> listSegmentsForDay(String cameraName, LocalDate date);
}

