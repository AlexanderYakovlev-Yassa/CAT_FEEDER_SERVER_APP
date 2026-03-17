package pl.torun.alex.feeder.feeder_server.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.torun.alex.feeder.feeder_server.dto.BuildInfoDto;

@RestController
@RequestMapping("/build-info")
public class BuildInfoController {

    @Value("${git.branch:unknown}")
    private String branch;

    @Value("${git.commit.id.abbrev:unknown}")
    private String commitId;

    @Value("${git.commit.time:unknown}")
    private String commitTime;

    /**
     * Returns build metadata (git branch, commit id, commit time).
     * Publicly accessible – no JWT required.
     */
    @GetMapping
    public ResponseEntity<BuildInfoDto> getBuildInfo() {
        return ResponseEntity.ok(new BuildInfoDto(branch, commitId, commitTime));
    }
}

