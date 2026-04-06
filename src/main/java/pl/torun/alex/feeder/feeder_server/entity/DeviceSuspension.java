package pl.torun.alex.feeder.feeder_server.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Represents a time period during which a device should NOT execute any scheduled feedings.
 * When the scheduler fires a feeding task, it checks if there is an active suspension
 * for the target device and skips the feeding if one is found.
 *
 * Both timestamps are stored as Instant (UTC epoch-based), so the server is
 * completely timezone-agnostic. Clients always communicate via Unix timestamps.
 */
@Entity
@Table(name = "device_suspensions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class DeviceSuspension {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The device whose feeding schedule is suspended.
     * Mapped to the device_id_fk column in DB.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id_fk", nullable = false)
    private Device device;

    /** Inclusive start of the suspension window (UTC instant). */
    @Column(name = "start_suspension", nullable = false)
    private Instant startSuspension;

    /** Inclusive end of the suspension window (UTC instant). */
    @Column(name = "end_suspension", nullable = false)
    private Instant endSuspension;
}

