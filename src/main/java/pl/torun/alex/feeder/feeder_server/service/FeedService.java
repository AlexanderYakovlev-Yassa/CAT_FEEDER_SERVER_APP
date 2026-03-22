package pl.torun.alex.feeder.feeder_server.service;

import pl.torun.alex.feeder.feeder_server.dto.FeedRequestDto;

public interface FeedService {

    /**
     * Sends an immediate feeding command to the device identified by
     * {@link FeedRequestDto#getSerialNumber()}.
     *
     * @param request DTO containing the device serial number and amount in grams
     * @throws java.util.NoSuchElementException if no device with the given serial number exists
     */
    void feed(FeedRequestDto request);
}

