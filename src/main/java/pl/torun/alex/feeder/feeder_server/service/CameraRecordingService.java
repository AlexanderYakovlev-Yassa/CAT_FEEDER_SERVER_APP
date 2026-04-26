package pl.torun.alex.feeder.feeder_server.service;

import java.util.Map;

public interface CameraRecordingService {

    /** Starts an FFmpeg recording process for the named camera. No-op if already running. */
    void startRecording(String cameraName);

    /** Gracefully stops the FFmpeg process for the named camera. */
    void stopRecording(String cameraName);

    /**
     * Returns the live recording state of every configured camera.
     * Key = camera name, value = true if the FFmpeg process is currently alive.
     */
    Map<String, Boolean> getRecordingStatus();
}

