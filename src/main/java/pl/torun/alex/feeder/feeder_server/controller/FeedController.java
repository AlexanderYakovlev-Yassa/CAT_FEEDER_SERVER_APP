package pl.torun.alex.feeder.feeder_server.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.torun.alex.feeder.feeder_server.dto.FeedRequestDto;
import pl.torun.alex.feeder.feeder_server.service.FeedService;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    /**
     * Sends an immediate feeding command to the device identified by its serial number.
     *
     * @param request DTO containing {@code serialNumber} and {@code amountInGrams}
     * @return 204 No Content on success, 404 Not Found if the device serial is unknown
     */
    @PostMapping
    @PreAuthorize("hasAuthority('read-feeders')")
    public ResponseEntity<Void> feed(@Valid @RequestBody FeedRequestDto request) {
        try {
            feedService.feed(request);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

