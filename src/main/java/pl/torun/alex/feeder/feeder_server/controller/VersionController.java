package pl.torun.alex.feeder.feeder_server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.torun.alex.feeder.feeder_server.dto.VersionEntryDto;
import pl.torun.alex.feeder.feeder_server.service.VersionService;

import java.util.List;

@RestController
@RequestMapping("/version")
@RequiredArgsConstructor
public class VersionController {

    private final VersionService versionService;

    /**
     * Returns the latest (current) version with its changelog.
     */
    @GetMapping
    public ResponseEntity<VersionEntryDto> getLatestVersion() {
        VersionEntryDto latest = versionService.getLatestVersion();
        if (latest == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(latest);
    }

    /**
     * Returns all versions with their changelogs (full history).
     */
    @GetMapping("/history")
    public ResponseEntity<List<VersionEntryDto>> getVersionHistory() {
        return ResponseEntity.ok(versionService.getAllVersions());
    }
}

