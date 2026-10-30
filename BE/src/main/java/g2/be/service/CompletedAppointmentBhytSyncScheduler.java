package g2.be.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "clinic.bhyt-sync.async.enabled", havingValue = "true", matchIfMissing = true)
public class CompletedAppointmentBhytSyncScheduler {

    private final BhytSyncService bhytSyncService;

    @Scheduled(
            initialDelayString = "${clinic.bhyt-sync.async.initial-delay-ms:15000}",
            fixedDelayString = "${clinic.bhyt-sync.async.scan-interval-ms:5000}")
    public void processQueuedBhytSyncs() {
        bhytSyncService.processCompletedAppointmentSyncs();
    }
}