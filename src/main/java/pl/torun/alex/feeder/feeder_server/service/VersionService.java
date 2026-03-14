package pl.torun.alex.feeder.feeder_server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import pl.torun.alex.feeder.feeder_server.dto.VersionEntryDto;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Slf4j
@Service
public class VersionService {

    private static final String VERSIONS_FILE = "versions.json";

    private final ObjectMapper objectMapper;
    private List<VersionEntryDto> cachedVersions;

    public VersionService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<VersionEntryDto> getAllVersions() {
        if (cachedVersions == null) {
            cachedVersions = loadVersions();
        }
        return cachedVersions;
    }

    public VersionEntryDto getLatestVersion() {
        List<VersionEntryDto> versions = getAllVersions();
        if (versions.isEmpty()) {
            return null;
        }
        return versions.get(0);
    }

    private List<VersionEntryDto> loadVersions() {
        try {
            InputStream is = new ClassPathResource(VERSIONS_FILE).getInputStream();
            return objectMapper.readValue(is, new TypeReference<>() {});
        } catch (IOException e) {
            log.error("Failed to load {}", VERSIONS_FILE, e);
            return List.of();
        }
    }
}

