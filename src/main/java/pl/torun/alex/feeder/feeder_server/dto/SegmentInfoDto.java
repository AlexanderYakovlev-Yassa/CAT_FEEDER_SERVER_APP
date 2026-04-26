package pl.torun.alex.feeder.feeder_server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Describes a single recorded MPEG-TS segment file.
 *
 * Example JSON:
 * {
 *   "filename":  "CatCamMaster_2026-04-26_14-30-00.ts",
 *   "date":      "2026-04-26",
 *   "startTime": "14:30:00",
 *   "sizeBytes": 157286400
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SegmentInfoDto {

    /** Raw filename (no path), e.g. {@code CatCamMaster_2026-04-26_14-30-00.ts}. */
    private String filename;

    /** Calendar date extracted from the filename. */
    private LocalDate date;

    /** Recording start time extracted from the filename. */
    private LocalTime startTime;

    /** File size in bytes at the time of the request. */
    private long sizeBytes;
}

