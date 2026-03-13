package pl.torun.alex.feeder.feeder_server.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "daily_scheduler")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"user", "device"})
public class DailyScheduler {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_name", nullable = false)
    private String taskName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "scheduler_times", joinColumns = @JoinColumn(name = "scheduler_id"))
    @Column(name = "scheduled_time")
    private List<FeedingMetadata> feedingMetadata;

    @Column(nullable = false)
    private Boolean active = true;
}
