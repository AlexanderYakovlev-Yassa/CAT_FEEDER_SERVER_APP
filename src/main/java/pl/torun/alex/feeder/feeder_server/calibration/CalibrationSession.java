package pl.torun.alex.feeder.feeder_server.calibration;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory representation of one calibration session.
 * Sessions are never persisted to the database – they live only as long as the
 * server process (or until confirmed / expired).
 */
@Data
@Builder
public class CalibrationSession {

    private static final AtomicLong ID_SEQ = new AtomicLong(1);
    private static final AtomicLong ATTEMPT_ID_SEQ = new AtomicLong(1);

    private final Long id;
    /** Serial number of the device being calibrated. */
    private final String deviceSerialNumber;
    /** Status lifecycle. */
    private CalibrationStatus status;
    /** Computed after all attempts are submitted (g/s). */
    private Float calculatedFeedConsumption;
    /** Std-dev across the per-attempt g/s values. */
    private Float standardDeviation;

    private final Instant createdAt;
    private Instant updatedAt;

    /** Ordered list of attempts (max {@code totalAttempts}). */
    @Builder.Default
    private List<CalibrationAttempt> attempts = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------

    public static CalibrationSession create(String deviceSerialNumber) {
        return CalibrationSession.builder()
                .id(ID_SEQ.getAndIncrement())
                .deviceSerialNumber(deviceSerialNumber)
                .status(CalibrationStatus.IN_PROGRESS)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Returns the current (latest) attempt, or null if none exist yet. */
    public CalibrationAttempt currentAttempt() {
        if (attempts.isEmpty()) return null;
        return attempts.get(attempts.size() - 1);
    }

    /** Creates and appends the next attempt; returns it. */
    public CalibrationAttempt addNextAttempt(int totalAttempts) {
        int nextNumber = attempts.size() + 1;
        CalibrationAttempt attempt = CalibrationAttempt.builder()
                .id(ATTEMPT_ID_SEQ.getAndIncrement())
                .attemptNumber(nextNumber)
                .build();
        attempts.add(attempt);
        updatedAt = Instant.now();
        return attempt;
    }

    public void touch() {
        updatedAt = Instant.now();
    }
}

