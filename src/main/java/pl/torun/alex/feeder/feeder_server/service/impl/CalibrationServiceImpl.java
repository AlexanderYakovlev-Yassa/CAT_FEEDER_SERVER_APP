package pl.torun.alex.feeder.feeder_server.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.torun.alex.feeder.feeder_server.calibration.CalibrationAttempt;
import pl.torun.alex.feeder.feeder_server.calibration.CalibrationSession;
import pl.torun.alex.feeder.feeder_server.calibration.CalibrationStatus;
import pl.torun.alex.feeder.feeder_server.dto.*;
import pl.torun.alex.feeder.feeder_server.entity.Device;
import pl.torun.alex.feeder.feeder_server.repository.DeviceRepository;
import pl.torun.alex.feeder.feeder_server.service.CalibrationService;
import pl.torun.alex.feeder.feeder_server.service.FeederClientService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalibrationServiceImpl implements CalibrationService {

    private final DeviceRepository deviceRepository;
    private final FeederClientService feederClientService;

    @Value("${calibration.duration-ms:2000}")
    private int calibrationDurationMs;

    @Value("${calibration.total-attempts:5}")
    private int totalAttempts;

    @Value("${calibration.session-expiry-minutes:30}")
    private int sessionExpiryMinutes;

    /** sessionId → CalibrationSession */
    private final Map<Long, CalibrationSession> sessions = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    @Override
    public StartCalibrationResponseDto startCalibration(StartCalibrationRequestDto request) {
        Device device = findDevice(request.getSerialNumber());

        // Reject if an active session already exists for this device
        boolean hasActive = sessions.values().stream()
                .anyMatch(s -> s.getDeviceSerialNumber().equals(request.getSerialNumber())
                        && isActive(s));
        if (hasActive) {
            throw new IllegalStateException(
                    "A calibration session is already in progress for device: " + request.getSerialNumber());
        }

        CalibrationSession session = CalibrationSession.create(request.getSerialNumber());
        CalibrationAttempt first = session.addNextAttempt(totalAttempts);
        sessions.put(session.getId(), session);

        feederClientService.sendCalibrationCommand(device, calibrationDurationMs);
        log.info("Started calibration session {} for device {}", session.getId(), device.getSerialNumber());

        return StartCalibrationResponseDto.builder()
                .sessionId(session.getId())
                .attemptId(first.getId())
                .attemptNumber(first.getAttemptNumber())
                .totalAttempts(totalAttempts)
                .calibrationDurationMs(calibrationDurationMs)
                .build();
    }

    @Override
    public SubmitAttemptResponseDto submitAttempt(SubmitAttemptRequestDto request) {
        CalibrationSession session = requireSession(request.getSessionId());
        requireStatus(session, CalibrationStatus.IN_PROGRESS);

        // Validate attempt belongs to session and is not yet measured
        CalibrationAttempt attempt = session.getAttempts().stream()
                .filter(a -> a.getId().equals(request.getAttemptId()))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                        "Attempt not found: " + request.getAttemptId() + " in session " + request.getSessionId()));

        if (attempt.isMeasured()) {
            throw new IllegalStateException(
                    "Attempt " + attempt.getAttemptNumber() + " already has a measurement");
        }

        attempt.setMeasuredGrams(request.getMeasuredGrams());
        session.touch();

        int completed = attempt.getAttemptNumber();
        int remaining = totalAttempts - completed;

        if (completed < totalAttempts) {
            // Fire next burst and create next attempt
            Device device = findDevice(session.getDeviceSerialNumber());
            CalibrationAttempt next = session.addNextAttempt(totalAttempts);
            feederClientService.sendCalibrationCommand(device, calibrationDurationMs);

            log.info("Session {} – attempt {}/{} recorded ({}g), next burst fired",
                    session.getId(), completed, totalAttempts, request.getMeasuredGrams());

            return SubmitAttemptResponseDto.builder()
                    .sessionId(session.getId())
                    .nextAttemptId(next.getId())
                    .attemptNumber(next.getAttemptNumber())
                    .remainingAttempts(remaining)
                    .readyForConfirmation(false)
                    .build();
        } else {
            // All attempts done – compute result
            computeResult(session);
            session.setStatus(CalibrationStatus.AWAITING_CONFIRMATION);

            log.info("Session {} – all {} attempts done. Calculated feedConsumption={}g/s, stdDev={}",
                    session.getId(), totalAttempts,
                    session.getCalculatedFeedConsumption(), session.getStandardDeviation());

            return SubmitAttemptResponseDto.builder()
                    .sessionId(session.getId())
                    .nextAttemptId(null)
                    .attemptNumber(completed)
                    .remainingAttempts(0)
                    .readyForConfirmation(true)
                    .build();
        }
    }

    @Override
    public CalibrationResultDto getResult(Long sessionId) {
        CalibrationSession session = requireSession(sessionId);
        requireStatus(session, CalibrationStatus.AWAITING_CONFIRMATION);

        Device device = findDevice(session.getDeviceSerialNumber());

        List<Float> measurements = session.getAttempts().stream()
                .map(a -> a.getMeasuredGrams() / (calibrationDurationMs / 1000f))
                .collect(Collectors.toList());

        return CalibrationResultDto.builder()
                .sessionId(session.getId())
                .deviceSerialNumber(session.getDeviceSerialNumber())
                .calculatedFeedConsumption(session.getCalculatedFeedConsumption())
                .standardDeviation(session.getStandardDeviation())
                .measurements(measurements)
                .currentFeedConsumption(device.getFeedConsumption())
                .build();
    }

    @Override
    @Transactional
    public void confirmCalibration(ConfirmCalibrationRequestDto request) {
        CalibrationSession session = requireSession(request.getSessionId());
        requireStatus(session, CalibrationStatus.AWAITING_CONFIRMATION);

        if (Boolean.TRUE.equals(request.getAccept())) {
            Device device = findDevice(session.getDeviceSerialNumber());
            device.setFeedConsumption(session.getCalculatedFeedConsumption());
            deviceRepository.save(device);
            session.setStatus(CalibrationStatus.ACCEPTED);
            log.info("Session {} accepted – device {} feedConsumption updated to {}g/s",
                    session.getId(), device.getSerialNumber(), session.getCalculatedFeedConsumption());
        } else {
            session.setStatus(CalibrationStatus.DECLINED);
            log.info("Session {} declined by user", session.getId());
        }
        session.touch();
        // Session stays in memory briefly; expiry task will eventually remove it
    }

    // -------------------------------------------------------------------------
    // Expiry task – runs every 5 minutes
    // -------------------------------------------------------------------------

    @Scheduled(fixedDelayString = "PT5M")
    public void expireStaleSessions() {
        Instant cutoff = Instant.now().minus(sessionExpiryMinutes, ChronoUnit.MINUTES);
        List<Long> toExpire = sessions.values().stream()
                .filter(s -> isActive(s) && s.getCreatedAt().isBefore(cutoff))
                .map(CalibrationSession::getId)
                .collect(Collectors.toList());

        toExpire.forEach(id -> {
            CalibrationSession s = sessions.get(id);
            if (s != null) {
                s.setStatus(CalibrationStatus.EXPIRED);
                s.touch();
                log.warn("Calibration session {} for device {} expired", id, s.getDeviceSerialNumber());
            }
        });

        // Purge finished sessions older than 2× expiry window
        Instant purgeCutoff = Instant.now().minus(sessionExpiryMinutes * 2L, ChronoUnit.MINUTES);
        sessions.entrySet().removeIf(e ->
                !isActive(e.getValue()) && e.getValue().getUpdatedAt().isBefore(purgeCutoff));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void computeResult(CalibrationSession session) {
        double durationSec = calibrationDurationMs / 1000.0;
        // Convert each measurement to g/s
        List<Double> gpsList = session.getAttempts().stream()
                .map(a -> a.getMeasuredGrams() / durationSec)
                .collect(Collectors.toList());

        double avg = gpsList.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = gpsList.stream()
                .mapToDouble(v -> Math.pow(v - avg, 2))
                .average().orElse(0);
        double stdDev = Math.sqrt(variance);

        session.setCalculatedFeedConsumption((float) avg);
        session.setStandardDeviation((float) stdDev);
    }

    private CalibrationSession requireSession(Long sessionId) {
        return Optional.ofNullable(sessions.get(sessionId))
                .orElseThrow(() -> new NoSuchElementException("Calibration session not found: " + sessionId));
    }

    private void requireStatus(CalibrationSession session, CalibrationStatus expected) {
        if (session.getStatus() == CalibrationStatus.EXPIRED) {
            throw new IllegalStateException("Calibration session " + session.getId() + " has expired");
        }
        if (session.getStatus() != expected) {
            throw new IllegalStateException(
                    "Session " + session.getId() + " is in state " + session.getStatus()
                            + ", expected " + expected);
        }
    }

    private Device findDevice(String serialNumber) {
        return deviceRepository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new NoSuchElementException("Device not found: " + serialNumber));
    }

    private boolean isActive(CalibrationSession s) {
        return s.getStatus() == CalibrationStatus.IN_PROGRESS
                || s.getStatus() == CalibrationStatus.AWAITING_CONFIRMATION;
    }
}

