package pl.torun.alex.feeder.feeder_server.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "app_user")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private boolean blocked;

    // Users can manage many devices; devices can belong to many users — join table user_to_device.
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_to_device",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "device_id")
    )
    private Set<Device> devices = new HashSet<>();

    // Users can access many cameras; cameras can be accessed by many users — join table user_to_camera.
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_to_camera",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "camera_id")
    )
    private Set<Camera> cameras = new HashSet<>();
}
