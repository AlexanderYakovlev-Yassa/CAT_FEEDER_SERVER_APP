package pl.torun.alex.feeder.feeder_server.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.torun.alex.feeder.feeder_server.dto.*;
import pl.torun.alex.feeder.feeder_server.service.CalibrationService;

import java.net.URI;
import java.util.NoSuchElementException;

/**
 * Endpoints for the feeder calibration workflow.
 *
 * <pre>
 * 1. POST /calibration/start              → fires first burst, returns sessionId + attemptId
 * 2. POST /calibration/attempt            → submit weight, fires next burst (or marks ready)
 * 3. GET  /calibration/{sessionId}/result → view computed feedConsumption + stdDev
 * 4. POST /calibration/confirm            → accept or decline
 * </pre>
 */
@RestController
@RequestMapping("/calibration")
@RequiredArgsConstructor
public class CalibrationController {

    private final CalibrationService calibrationService;

    /**
     * Start a new calibration session.
     * Fires the first 2000 ms burst and returns the session / attempt IDs.
     */
    @PostMapping("/start")
    @PreAuthorize("hasAuthority('manage-feeders')")
    public ResponseEntity<?> start(@Valid @RequestBody StartCalibrationRequestDto request) {
        try {
            StartCalibrationResponseDto response = calibrationService.startCalibration(request);
            return ResponseEntity
                    .created(URI.create("/calibration/" + response.getSessionId() + "/result"))
                    .body(response);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Submit the weighed grams for the current attempt.
     * If more attempts remain, the server fires the next burst automatically.
     * When {@code readyForConfirmation=true}, proceed to GET /result then POST /confirm.
     */
    @PostMapping("/attempt")
    @PreAuthorize("hasAuthority('manage-feeders')")
    public ResponseEntity<?> submitAttempt(@Valid @RequestBody SubmitAttemptRequestDto request) {
        try {
            SubmitAttemptResponseDto response = calibrationService.submitAttempt(request);
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Retrieve the computed calibration result (available once all attempts are submitted).
     */
    @GetMapping("/{sessionId}/result")
    @PreAuthorize("hasAuthority('manage-feeders')")
    public ResponseEntity<?> getResult(@PathVariable Long sessionId) {
        try {
            CalibrationResultDto result = calibrationService.getResult(sessionId);
            return ResponseEntity.ok(result);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Accept or decline the computed feedConsumption.
     * On accept, the device's feedConsumption is updated in the database.
     */
    @PostMapping("/confirm")
    @PreAuthorize("hasAuthority('manage-feeders')")
    public ResponseEntity<?> confirm(@Valid @RequestBody ConfirmCalibrationRequestDto request) {
        try {
            calibrationService.confirmCalibration(request);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}

