package pl.torun.alex.feeder.feeder_server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Summary of all recorded segments for a single calendar day.
 *
 * Example JSON:
 * {
 *   "date":           "2026-04-26",
 *   "segmentCount":   6,
 *   "totalSizeBytes": 943718400,
 *   "segments": [ ... ]
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecordingDayDto {

    private LocalDate date;

    /** Number of .ts segment files recorded on this date. */
    private int segmentCount;

    /** Combined size of all segments for this day in bytes. */
    private long totalSizeBytes;

    /** Ordered list of individual segments (ascending by start time). */
    private List<SegmentInfoDto> segments;
}

