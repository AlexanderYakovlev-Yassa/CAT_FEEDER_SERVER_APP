package pl.torun.alex.feeder.feeder_server.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "daily_scheduler")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    @ElementCollection
    @CollectionTable(name = "scheduler_times", joinColumns = @JoinColumn(name = "scheduler_id"))
    @Column(name = "scheduled_time")
    private List<LocalTime> scheduledTimes;

    @Column(nullable = false)
    private Boolean active = true;
}
