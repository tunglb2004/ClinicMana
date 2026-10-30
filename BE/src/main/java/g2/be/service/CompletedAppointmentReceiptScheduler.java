package g2.be.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "clinic.receipt.async.enabled", havingValue = "true", matchIfMissing = true)
public class CompletedAppointmentReceiptScheduler {

    private final ReceiptIssuanceService receiptIssuanceService;

    @Scheduled(
            initialDelayString = "${clinic.receipt.async.initial-delay-ms:15000}",
            fixedDelayString = "${clinic.receipt.async.scan-interval-ms:3000}"
    )
    public void processCompletedAppointmentReceipts() {
        log.trace("Scanning completed appointments for pending receipt issuance");
        receiptIssuanceService.processCompletedAppointmentReceipts();
    }
}