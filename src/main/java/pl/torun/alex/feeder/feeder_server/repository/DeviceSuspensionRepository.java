package pl.torun.alex.feeder.feeder_server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.torun.alex.feeder.feeder_server.entity.DeviceSuspension;

import java.time.Instant;
import java.util.List;

@Repository
public interface DeviceSuspensionRepository extends JpaRepository<DeviceSuspension, Long> {

    /** Returns all suspensions registered for a specific device. */
    List<DeviceSuspension> findByDeviceId(Long deviceId);

    /**
     * Returns true when the device has any suspension window that overlaps
     * the given Instant (UTC epoch-based, so timezone-safe).
     *
     * Overlap condition: start <= at <= end
     *
     * The Instant parameter comes directly from Instant.now() in the scheduler,
     * ensuring the comparison is always in absolute UTC time regardless of
     * where the server or the client is running.
     */
    @Query("""
        SELECT COUNT(s) > 0
        FROM DeviceSuspension s
        WHERE s.device.id    = :deviceId
          AND s.startSuspension <= :at
          AND s.endSuspension   >= :at
        """)
    boolean existsActiveSuspension(@Param("deviceId") Long deviceId,
                                   @Param("at") Instant at);
}
