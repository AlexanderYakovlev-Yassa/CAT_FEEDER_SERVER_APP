package pl.torun.alex.feeder.feeder_server.calibration;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * One measurement attempt within a {@link CalibrationSession}.
 */
@Data
@Builder
public class CalibrationAttempt {

    private final Long id;
    private final Integer attemptNumber;
    /** Set by user after weighing the dispensed feed. */
    private Float measuredGrams;
    private final Instant createdAt;

    public boolean isMeasured() {
        return measuredGrams != null;
    }
}

