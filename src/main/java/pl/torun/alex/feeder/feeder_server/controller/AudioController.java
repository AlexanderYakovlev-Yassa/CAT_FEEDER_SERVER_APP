package pl.torun.alex.feeder.feeder_server.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.torun.alex.feeder.feeder_server.dto.AudioPresetDto;
import pl.torun.alex.feeder.feeder_server.service.AudioService;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/audio")
@RequiredArgsConstructor
public class AudioController {

    private final AudioService audioService;

    @GetMapping("/presets")
    public ResponseEntity<List<AudioPresetDto>> getPresets() {
        return ResponseEntity.ok(audioService.getPresets());
    }

    @PostMapping("/play/{presetId}")
    public ResponseEntity<Map<String, Object>> playPreset(@PathVariable String presetId) {
        log.info("Request to play preset: {}", presetId);
        try {
            audioService.playPreset(presetId);
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "presetId", presetId));
        } catch (Exception e) {
            log.error("Failed to play preset {}: {}", presetId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "ERROR", "message", e.getMessage() != null ? e.getMessage() : "Playback failed"));
        }
    }

    @PostMapping(value = "/voice", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<Map<String, Object>> playVoiceMultipart(@RequestParam("file") MultipartFile file) {
        try {
            log.info("Received voice file: name={}, size={}, contentType={}",
                    file.getOriginalFilename(), file.getSize(), file.getContentType());
            audioService.playVoice(file.getBytes(), file.getContentType());
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "bytes", file.getSize()));
        } catch (Exception e) {
            log.error("Error processing voice file: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }

    @PostMapping(value = "/voice", consumes = {"audio/*", MediaType.APPLICATION_OCTET_STREAM_VALUE})
    public ResponseEntity<Map<String, Object>> playVoiceRaw(
            @RequestBody byte[] audioData,
            @RequestHeader(value = "Content-Type", required = false) String contentType) {
        try {
            log.info("Received raw voice stream: size={}, contentType={}", audioData.length, contentType);
            audioService.playVoice(audioData, contentType);
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "bytes", audioData.length));
        } catch (Exception e) {
            log.error("Error processing raw voice stream: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }

    @PostMapping(value = "/custom", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<AudioPresetDto> createCustomPreset(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "name", required = false) String name) {
        try {
            log.info("Request to create custom preset: name={}, size={}, contentType={}",
                    name, file.getSize(), file.getContentType());
            AudioPresetDto created = audioService.saveCustomPreset(name, file.getBytes(), file.getContentType());
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            log.error("Error creating custom preset: {}", e.getMessage(), e);
            throw new RuntimeException("Could not create custom preset: " + e.getMessage(), e);
        }
    }

    @DeleteMapping("/custom/{id}")
    public ResponseEntity<Map<String, Object>> deleteCustomPreset(@PathVariable Long id) {
        log.info("Request to delete custom preset: {}", id);
        audioService.deleteCustomPreset(id);
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "deletedId", id));
    }
}
