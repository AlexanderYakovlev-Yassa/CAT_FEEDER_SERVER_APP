package pl.torun.alex.feeder.feeder_server.service.impl;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.torun.alex.feeder.feeder_server.entity.DailyScheduler;
import pl.torun.alex.feeder.feeder_server.entity.Device;
import pl.torun.alex.feeder.feeder_server.entity.FeedingMetadata;
import pl.torun.alex.feeder.feeder_server.repository.DailySchedulerRepository;
import pl.torun.alex.feeder.feeder_server.repository.DeviceRepository;
import pl.torun.alex.feeder.feeder_server.service.FeederClientService;
import pl.torun.alex.feeder.feeder_server.service.MainScheduleService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class MainScheduleServiceImpl implements MainScheduleService {

    private final DeviceRepository deviceRepository;
    private final DailySchedulerRepository dailySchedulerRepository;
    private final FeederClientService feederClientService;
    private final ThreadPoolTaskScheduler taskScheduler;

    private final Map<Long, DailyScheduler> schedulerServiceMap = new HashMap<>();
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new HashMap<>();

    @Override
    @PostConstruct
    public void init() {
        log.info("Initializing feeding schedules...");
        loadAndScheduleAll();
    }

    @Override
    public void reschedule() {
        log.info("Rescheduling all feedings due to schedule change...");
        cancelAllScheduledTasks();
        loadAndScheduleAll();
    }

    @Scheduled(cron = "0 0 0 * * ?") // Run at midnight every day
    public void rescheduleDaily() {
        log.info("Rescheduling all feedings for new day...");
        cancelAllScheduledTasks();
        loadAndScheduleAll();
    }

    @Transactional(readOnly = true)
    public void loadAndScheduleAll() {
        schedulerServiceMap.clear();

        List<Device> devices = deviceRepository.findAll();
        LocalDate today = LocalDate.now();

        for (Device device : devices) {
            Optional<DailyScheduler> schedulerOptional = dailySchedulerRepository.findByDeviceAndActiveTrue(device);
            if (schedulerOptional.isPresent()) {
                DailyScheduler scheduler = schedulerOptional.get();

                System.out.println("Found active scheduler for device: " + device.getName());
                System.out.println("Feeding metadata: " + scheduler.getFeedingMetadata());

                // Eagerly initialize the feedingMetadata collection within transaction
                List<FeedingMetadata> feedingMetadata = scheduler.getFeedingMetadata();

                if (feedingMetadata == null || feedingMetadata.isEmpty()) {
                    log.warn("No feeding times configured for device: {}", device.getName());
                    continue;
                }

                // Store scheduler for later use
                schedulerServiceMap.put(device.getId(), scheduler);
                log.info("Loaded scheduler for device: {} with {} feeding times",
                    device.getName(), feedingMetadata.size());

                // Schedule each feeding task
                for (FeedingMetadata metadata : feedingMetadata) {
                    scheduleFeedingTask(device, metadata, today);
                }

                // Notify device that schedule has been initialized
                feederClientService.sendScheduleBeenInitCommand(device);
            } else {
                log.warn("No active scheduler found for device: {}", device.getName());
            }
        }
    }

    private void scheduleFeedingTask(Device device, FeedingMetadata metadata, LocalDate date) {
        LocalDateTime scheduledDateTime = LocalDateTime.of(date, metadata.getFeedingTime());
        LocalDateTime now = LocalDateTime.now();

        // Only schedule if the time hasn't passed yet today
        if (scheduledDateTime.isAfter(now)) {
            var scheduledInstant = scheduledDateTime.atZone(ZoneId.systemDefault()).toInstant();

            String taskKey = device.getId() + ":" + metadata.getFeedingTime();

            ScheduledFuture<?> scheduledTask = taskScheduler.schedule(
                () -> executeFeedingTask(device, metadata),
                scheduledInstant
            );

            scheduledTasks.put(taskKey, scheduledTask);

            log.info("Scheduled feeding for device '{}' at {} for {}g",
                device.getName(), metadata.getFeedingTime(), metadata.getAmountInGrams());
        } else {
            log.debug("Skipping past feeding time {} for device '{}'",
                metadata.getFeedingTime(), device.getName());
        }
    }

    private void executeFeedingTask(Device device, FeedingMetadata metadata) {
        try {
            log.info("Executing scheduled feeding for device '{}' - amount: {}g",
                device.getName(), metadata.getAmountInGrams());
            feederClientService.sendFeedingCommand(device, metadata.getAmountInGrams());
        } catch (Exception e) {
            log.error("Error executing feeding task for device '{}': {}",
                device.getName(), e.getMessage(), e);
        }
    }

    private void cancelAllScheduledTasks() {
        log.info("Cancelling all scheduled tasks...");
        scheduledTasks.values().forEach(task -> {
            if (task != null && !task.isDone()) {
                task.cancel(false);
            }
        });
        scheduledTasks.clear();
    }
}
