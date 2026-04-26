package pl.torun.alex.feeder.feeder_server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.torun.alex.feeder.feeder_server.service.CameraRecordingService;

import java.util.Map;

/**
 * REST API for controlling IP camera recording.
 *
 * <pre>
 * POST /camera/{name}/start  – start recording for the named camera
 * POST /camera/{name}/stop   – stop  recording for the named camera
 * GET  /camera/status        – recording state of all configured cameras
 * </pre>
 *
 * The {@code name} path variable must match the {@code name} column in the {@code camera} table.
 */
@RestController
@RequestMapping("/camera")
@RequiredArgsConstructor
public class CameraRecordingController {

    private final CameraRecordingService cameraRecordingService;

    /**
     * Starts the FFmpeg recording process for the given camera.
     * If the camera is already recording this is a silent no-op.
     */
    @PostMapping("/{name}/start")
    @PreAuthorize("hasAuthority('manage-schedule')")
    public ResponseEntity<String> start(@PathVariable String name) {
        cameraRecordingService.startRecording(name);
        return ResponseEntity.ok("Recording started for camera: " + name);
    }

    /**
     * Stops the FFmpeg recording process for the given camera.
     * The last open segment will be truncated at the point of termination.
     */
    @PostMapping("/{name}/stop")
    @PreAuthorize("hasAuthority('manage-schedule')")
    public ResponseEntity<String> stop(@PathVariable String name) {
        cameraRecordingService.stopRecording(name);
        return ResponseEntity.ok("Recording stopped for camera: " + name);
    }

    /**
     * Returns the current recording state of every configured camera.
     *
     * <p>Response body example:
     * <pre>{ "CatCamMaster": true }</pre>
     */
    @GetMapping("/status")
    @PreAuthorize("hasAuthority('read-schedule')")
    public ResponseEntity<Map<String, Boolean>> status() {
        return ResponseEntity.ok(cameraRecordingService.getRecordingStatus());
    }
}

