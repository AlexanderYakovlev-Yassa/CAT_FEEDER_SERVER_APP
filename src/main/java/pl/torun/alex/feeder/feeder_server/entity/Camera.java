package pl.torun.alex.feeder.feeder_server.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "camera")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = "users")
public class Camera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique logical name — used in file names and REST API paths. */
    @Column(nullable = false, unique = true)
    private String name;

    /** Full RTSP URL including credentials. */
    @Column(nullable = false)
    private String rtspUrl;

    /** Absolute path to the directory where .ts segment files will be written. */
    @Column(nullable = false)
    private String storagePath;

    /**
     * When true the recording starts automatically on application startup
     * and is restarted automatically whenever the FFmpeg process dies.
     */
    @Column(nullable = false)
    private boolean autoStart = false;

    /** Users who have access to this camera. */
    @ManyToMany(mappedBy = "cameras", fetch = FetchType.LAZY)
    private Set<AppUser> users = new HashSet<>();
}

