package pl.torun.alex.feeder.feeder_server.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "device")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = "users")
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "serial_number", unique = true, nullable = false)
    private String serialNumber;

    // Previously a ManyToOne to AppUser; changed to bidirectional ManyToMany using join table user_to_device.
    @ManyToMany(mappedBy = "devices", fetch = FetchType.LAZY)
    private Set<AppUser> users = new HashSet<>();

    private Float feedConsumption; // in grams per second
}
