package pl.torun.alex.feeder.feeder_server.service;

import pl.torun.alex.feeder.feeder_server.dto.*;

public interface CalibrationService {

    /**
     * Starts a new calibration session for the given device and fires the first burst.
     */
    StartCalibrationResponseDto startCalibration(StartCalibrationRequestDto request);

    /**
     * Records the user-measured weight for the current attempt.
     * If more attempts remain, fires the next burst.
     * After the final attempt, computes feedConsumption + stdDev.
     */
    SubmitAttemptResponseDto submitAttempt(SubmitAttemptRequestDto request);

    /**
     * Returns the computed calibration result for a session in AWAITING_CONFIRMATION state.
     */
    CalibrationResultDto getResult(Long sessionId);

    /**
     * Accepts or declines the computed feedConsumption.
     * On accept, updates the Device entity in the database.
     */
    void confirmCalibration(ConfirmCalibrationRequestDto request);
}

