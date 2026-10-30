package g2.be.service;

import g2.be.dto.response.ReceiptResponse;
import g2.be.entity.Invoice;
import g2.be.entity.ServiceOrder;
import g2.be.exception.AppException;
import g2.be.exception.ErrorCode;
import g2.be.repository.InvoiceRepository;
import g2.be.repository.ServiceOrderRepository;
import g2.be.util.ClinicTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReceiptIssuanceService {

    private static final DateTimeFormatter RECEIPT_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String APPOINTMENT_STATUS_COMPLETED = "COMPLETED";
    private static final String PAYMENT_STATUS_PAID = "PAID";
    private static final String RECEIPT_STATUS_NONE = "NONE";
    private static final String RECEIPT_STATUS_FAILED = "FAILED";
    private static final String RECEIPT_STATUS_PROCESSING = "PROCESSING";
    private static final String RECEIPT_STATUS_ISSUED = "ISSUED";

    private final InvoiceRepository invoiceRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final EmailService emailService;
    private final SepayEinvoiceClient sepayEinvoiceClient;
    private final PlatformTransactionManager transactionManager;

    @Value("${clinic.receipt.async.batch-size:10}")
    private int batchSize;

    @Value("${clinic.receipt.async.processing-timeout-seconds:300}")
    private long processingTimeoutSeconds;

    public void processCompletedAppointmentReceipts() {
        LocalDateTime staleBefore = processingCutoff();
        List<String> recordIds = invoiceRepository.findRecordIdsEligibleForReceiptIssuance(
                APPOINTMENT_STATUS_COMPLETED,
                PAYMENT_STATUS_PAID,
                List.of(RECEIPT_STATUS_NONE, RECEIPT_STATUS_FAILED),
                RECEIPT_STATUS_PROCESSING,
                staleBefore,
                PageRequest.of(0, batchSize));

        for (String recordId : recordIds) {
            try {
                runInRequiresNewTransaction(() -> {
                    issueReceiptForRecord(recordId, null, false);
                    return null;
                });
            } catch (AppException exception) {
                log.debug("Skip async receipt issuance for record {}: {}", recordId, exception.getMessage());
            } catch (RuntimeException exception) {
                log.error("Async receipt issuance failed for record {}", recordId, exception);
                markReceiptFailedForRecord(recordId, exception);
            }
        }
    }

    public ReceiptResponse issueReceiptNow(String invoiceId) {
        try {
            return runInRequiresNewTransaction(() -> issueReceiptForInvoice(invoiceId, true));
        } catch (AppException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error("Manual receipt issuance failed for invoice {}", invoiceId, exception);
            markReceiptFailedForInvoiceScope(invoiceId, exception);
            throw exception;
        }
    }

    private ReceiptResponse issueReceiptForRecord(String recordId,
            String preferredInvoiceId,
            boolean failWhenBusy) {
        List<Invoice> paidInvoices = getPaidInvoicesForMedicalRecord(recordId);
        if (paidInvoices.isEmpty()) {
            log.debug("No paid invoices found for record {}", recordId);
            return null;
        }

        Invoice anchorInvoice = selectAnchorInvoice(paidInvoices, preferredInvoiceId);
        validateReceiptEligibility(anchorInvoice);
        ensurePaidInvoicesNotIssued(paidInvoices);

        LocalDateTime now = ClinicTime.now();
        if (!tryClaimRecord(recordId, now)) {
            if (!failWhenBusy) {
                log.debug("Could not claim record {} for receipt issuance", recordId);
                return null;
            }

            List<Invoice> refreshedPaidInvoices = getPaidInvoicesForMedicalRecord(recordId);
            if (!refreshedPaidInvoices.isEmpty()) {
                ensurePaidInvoicesNotIssued(refreshedPaidInvoices);
            }
            throw new AppException(ErrorCode.RECEIPT_ISSUANCE_IN_PROGRESS);
        }

        List<Invoice> freshPaidInvoices = getPaidInvoicesForMedicalRecord(recordId);
        if (freshPaidInvoices.isEmpty()) {
            log.debug("No paid invoices remain after claim for record {}", recordId);
            return null;
        }

        Invoice claimedAnchor = selectAnchorInvoice(freshPaidInvoices, preferredInvoiceId);
        List<ServiceOrder> activeOrders = getReceiptOrdersForMedicalRecord(claimedAnchor, freshPaidInvoices);
        Invoice receiptSnapshot = buildReceiptSnapshot(claimedAnchor, freshPaidInvoices, activeOrders);
        SepayEinvoiceClient.IssuedEinvoice issuedEinvoice = sepayEinvoiceClient.issueInvoice(receiptSnapshot,
                activeOrders, now);

        String receiptNumber = nextReceiptNumber("PT-" + now.toLocalDate().format(RECEIPT_DATE_FORMATTER));
        for (Invoice paidInvoice : freshPaidInvoices) {
            boolean isAnchor = claimedAnchor.getInvoiceId().equals(paidInvoice.getInvoiceId());
            applyReceiptData(paidInvoice, receiptNumber, now, issuedEinvoice, isAnchor);
        }
        applyReceiptData(receiptSnapshot, receiptNumber, now, issuedEinvoice, true);
        invoiceRepository.saveAll(freshPaidInvoices);

        log.info("Consolidated receipt issued for record {}: receiptNumber={}, eInvoiceCode={}, invoiceCount={}",
                recordId, receiptNumber, issuedEinvoice.referenceCode(), freshPaidInvoices.size());

        ReceiptResponse receiptResponse = ReceiptResponse.fromEntity(receiptSnapshot, activeOrders);
        sendIssuedReceiptEmail(receiptResponse);
        return receiptResponse;
    }

    /**
     * Issue receipt for a specific invoice (manual trigger / backward
     * compatibility).
     * Still consolidates all paid invoices for the same medical record.
     */
    private ReceiptResponse issueReceiptForInvoice(String invoiceId, boolean failWhenBusy) {
        Invoice currentInvoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new AppException(ErrorCode.INVOICE_NOT_FOUND));
        validateReceiptEligibility(currentInvoice);
        ensureRecordReceiptNotIssued(currentInvoice);

        String recordId = currentInvoice.getMedicalRecord() != null
                ? currentInvoice.getMedicalRecord().getRecordId()
                : null;
        if (recordId == null || recordId.isBlank()) {
            throw new AppException(ErrorCode.INVOICE_NOT_FOUND);
        }

        return issueReceiptForRecord(recordId, invoiceId, failWhenBusy);
    }

    private void validateReceiptEligibility(Invoice invoice) {
        if (!PAYMENT_STATUS_PAID.equals(invoice.getPaymentStatus())) {
            throw new AppException(ErrorCode.RECEIPT_UNPAID_INVOICE);
        }

        if (RECEIPT_STATUS_ISSUED.equals(invoice.getReceiptStatus())) {
            throw new AppException(ErrorCode.RECEIPT_ALREADY_ISSUED);
        }

        String appointmentStatus = invoice.getMedicalRecord() != null
                && invoice.getMedicalRecord().getAppointment() != null
                        ? invoice.getMedicalRecord().getAppointment().getStatus()
                        : null;
        if (!APPOINTMENT_STATUS_COMPLETED.equals(appointmentStatus)) {
            throw new AppException(ErrorCode.RECEIPT_APPOINTMENT_NOT_COMPLETED);
        }
    }

    private boolean tryClaimRecord(String recordId, LocalDateTime now) {
        return invoiceRepository.claimReceiptIssuanceByRecord(
                recordId,
                PAYMENT_STATUS_PAID,
                RECEIPT_STATUS_NONE,
                RECEIPT_STATUS_FAILED,
                RECEIPT_STATUS_PROCESSING,
                now,
                processingCutoff()) > 0;
    }

    private void ensureRecordReceiptNotIssued(Invoice invoice) {
        List<Invoice> paidInvoices = getPaidInvoicesForMedicalRecord(invoice);
        ensurePaidInvoicesNotIssued(paidInvoices);
    }

    private void applyReceiptData(Invoice invoice,
            String receiptNumber,
            LocalDateTime issuedAt,
            SepayEinvoiceClient.IssuedEinvoice issuedEinvoice,
            boolean includeReceiptNumber) {
        if (includeReceiptNumber) {
            invoice.setReceiptNumber(receiptNumber);
        }
        invoice.setEInvoiceCode(issuedEinvoice.referenceCode());
        invoice.setEInvoiceUrl(issuedEinvoice.pdfUrl());
        invoice.setReceiptStatus(RECEIPT_STATUS_ISSUED);
        invoice.setReceiptIssuedAt(issuedAt);
        invoice.setReceiptProcessingStartedAt(null);
        invoice.setReceiptErrorMessage(null);
    }

    private String nextReceiptNumber(String datePrefix) {
        String receiptPrefix = datePrefix + "-";
        String latestReceiptNumber = invoiceRepository
                .findTopByReceiptNumberStartingWithOrderByReceiptNumberDesc(datePrefix)
                .map(Invoice::getReceiptNumber)
                .orElse(null);

        long nextSequence = 1;
        if (latestReceiptNumber != null && !latestReceiptNumber.isBlank()) {
            if (!latestReceiptNumber.startsWith(receiptPrefix)) {
                throw new IllegalStateException("Unexpected receipt number format: " + latestReceiptNumber);
            }

            String sequencePart = latestReceiptNumber.substring(receiptPrefix.length());
            try {
                nextSequence = Long.parseLong(sequencePart) + 1;
            } catch (NumberFormatException exception) {
                throw new IllegalStateException("Invalid receipt number suffix: " + latestReceiptNumber, exception);
            }
        }

        return String.format("%s-%04d", datePrefix, nextSequence);
    }

    private void sendIssuedReceiptEmail(ReceiptResponse receiptResponse) {
        if (receiptResponse == null
                || receiptResponse.getPatientEmail() == null || receiptResponse.getPatientEmail().isBlank()
                || receiptResponse.getEInvoiceUrl() == null || receiptResponse.getEInvoiceUrl().isBlank()) {
            return;
        }

        try {
            emailService.sendReceiptIssuedEmail(
                    receiptResponse.getPatientEmail(),
                    receiptResponse.getPatientName(),
                    receiptResponse.getInvoiceId(),
                    receiptResponse.getEInvoiceCode(),
                    receiptResponse.getEInvoiceUrl(),
                    receiptResponse.getReceiptIssuedAt(),
                    receiptResponse.getCustomerPaid());
            log.info("Receipt email queued for invoice {} to {}",
                    receiptResponse.getInvoiceId(),
                    receiptResponse.getPatientEmail());
        } catch (Exception exception) {
            log.error("Failed to queue receipt email for invoice {}", receiptResponse.getInvoiceId(), exception);
        }
    }

    private void markReceiptFailed(String invoiceId, RuntimeException exception) {
        String errorMessage = trimErrorMessage(exception.getMessage());
        try {
            runInRequiresNewTransaction(() -> {
                invoiceRepository.markReceiptIssuanceFailed(
                        invoiceId,
                        RECEIPT_STATUS_FAILED,
                        RECEIPT_STATUS_ISSUED,
                        errorMessage);
                return null;
            });
        } catch (RuntimeException markFailureException) {
            log.error("Unable to persist failed receipt state for invoice {}", invoiceId, markFailureException);
        }
    }

    private void markReceiptFailedForInvoiceScope(String invoiceId, RuntimeException exception) {
        try {
            Optional<Invoice> invoice = invoiceRepository.findById(invoiceId);
            String recordId = invoice
                    .map(Invoice::getMedicalRecord)
                    .map(medicalRecord -> medicalRecord.getRecordId())
                    .orElse(null);
            if (recordId != null && !recordId.isBlank()) {
                markReceiptFailedForRecord(recordId, exception);
                return;
            }
        } catch (RuntimeException lookupException) {
            log.warn("Unable to resolve record scope for failed invoice {}", invoiceId, lookupException);
        }

        markReceiptFailed(invoiceId, exception);
    }

    private void markReceiptFailedForRecord(String recordId, RuntimeException exception) {
        String errorMessage = trimErrorMessage(exception.getMessage());
        try {
            runInRequiresNewTransaction(() -> {
                List<Invoice> invoices = invoiceRepository.findByMedicalRecord_RecordId(recordId);
                for (Invoice invoice : invoices) {
                    if (!RECEIPT_STATUS_ISSUED.equals(invoice.getReceiptStatus())) {
                        invoiceRepository.markReceiptIssuanceFailed(
                                invoice.getInvoiceId(),
                                RECEIPT_STATUS_FAILED,
                                RECEIPT_STATUS_ISSUED,
                                errorMessage);
                    }
                }
                return null;
            });
        } catch (RuntimeException markFailureException) {
            log.error("Unable to persist failed receipt state for record {}", recordId, markFailureException);
        }
    }

    private List<Invoice> getPaidInvoicesForMedicalRecord(Invoice invoice) {
        if (invoice == null) {
            return List.of();
        }

        if (invoice.getMedicalRecord() == null || invoice.getMedicalRecord().getRecordId() == null) {
            return List.of(invoice);
        }

        List<Invoice> paidInvoices = getPaidInvoicesForMedicalRecord(invoice.getMedicalRecord().getRecordId());
        return paidInvoices.isEmpty() ? List.of(invoice) : paidInvoices;
    }

    private List<Invoice> getPaidInvoicesForMedicalRecord(String recordId) {
        if (recordId == null || recordId.isBlank()) {
            return List.of();
        }

        return invoiceRepository.findByMedicalRecord_RecordId(recordId).stream()
                .filter(relatedInvoice -> PAYMENT_STATUS_PAID.equals(relatedInvoice.getPaymentStatus()))
                .sorted(Comparator.comparing(Invoice::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Invoice::getInvoiceId, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private void ensurePaidInvoicesNotIssued(List<Invoice> paidInvoices) {
        boolean hasIssuedReceipt = paidInvoices.stream()
                .anyMatch(relatedInvoice -> RECEIPT_STATUS_ISSUED.equals(relatedInvoice.getReceiptStatus()));
        if (hasIssuedReceipt) {
            throw new AppException(ErrorCode.RECEIPT_ALREADY_ISSUED);
        }
    }

    private Invoice selectAnchorInvoice(List<Invoice> paidInvoices, String preferredInvoiceId) {
        if (paidInvoices == null || paidInvoices.isEmpty()) {
            throw new AppException(ErrorCode.INVOICE_NOT_FOUND);
        }

        if (preferredInvoiceId != null && !preferredInvoiceId.isBlank()) {
            for (Invoice invoice : paidInvoices) {
                if (preferredInvoiceId.equals(invoice.getInvoiceId())) {
                    return invoice;
                }
            }
        }

        return paidInvoices.get(0);
    }

    private List<ServiceOrder> getReceiptOrdersForMedicalRecord(Invoice invoice, List<Invoice> paidInvoices) {
        if (invoice == null || invoice.getMedicalRecord() == null || invoice.getMedicalRecord().getRecordId() == null) {
            return List.of();
        }

        Set<String> paidInvoiceIds = paidInvoices.stream()
            .map(Invoice::getInvoiceId)
            .filter(Objects::nonNull)
            .collect(java.util.stream.Collectors.toSet());

        if (paidInvoiceIds.isEmpty()) {
            return List.of();
        }

        return serviceOrderRepository.findByMedicalRecord_RecordId(invoice.getMedicalRecord().getRecordId()).stream()
                .filter(order -> !"CANCELLED".equals(order.getStatus()))
            .filter(order -> order.getInvoiceId() != null && paidInvoiceIds.contains(order.getInvoiceId()))
                .sorted(Comparator.comparing(ServiceOrder::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private Invoice buildReceiptSnapshot(Invoice baseInvoice,
            List<Invoice> paidInvoices,
            List<ServiceOrder> activeOrders) {
        BigDecimal totalServiceFee = activeOrders.stream()
                .map(ServiceOrder::getService)
                .filter(Objects::nonNull)
                .map(g2.be.entity.Service::getPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal insurancePaid = paidInvoices.stream()
                .map(Invoice::getInsurancePaid)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal customerPaid = paidInvoices.stream()
                .map(Invoice::getCustomerPaid)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDateTime paidAt = paidInvoices.stream()
                .map(Invoice::getPaidAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(baseInvoice.getPaidAt());

        return Invoice.builder()
                .invoiceId(baseInvoice.getInvoiceId())
                .medicalRecord(baseInvoice.getMedicalRecord())
                .cashierId(baseInvoice.getCashierId())
                .totalServiceFee(totalServiceFee.compareTo(BigDecimal.ZERO) > 0 ? totalServiceFee
                        : baseInvoice.getTotalServiceFee())
                .insurancePaid(insurancePaid)
                .customerPaid(
                        customerPaid.compareTo(BigDecimal.ZERO) > 0 ? customerPaid : baseInvoice.getCustomerPaid())
                .paymentStatus(baseInvoice.getPaymentStatus())
                .paymentMethod(baseInvoice.getPaymentMethod())
                .transactionId(baseInvoice.getTransactionId())
                .paidAt(paidAt)
                .gatewayPayload(baseInvoice.getGatewayPayload())
                .bhytSyncStatus(baseInvoice.getBhytSyncStatus())
                .syncErrorMessage(baseInvoice.getSyncErrorMessage())
                .receiptNumber(baseInvoice.getReceiptNumber())
                .eInvoiceCode(baseInvoice.getEInvoiceCode())
                .eInvoiceUrl(baseInvoice.getEInvoiceUrl())
                .receiptIssuedAt(baseInvoice.getReceiptIssuedAt())
                .receiptStatus(baseInvoice.getReceiptStatus())
                .receiptProcessingStartedAt(baseInvoice.getReceiptProcessingStartedAt())
                .receiptErrorMessage(baseInvoice.getReceiptErrorMessage())
                .createdAt(baseInvoice.getCreatedAt())
                .build();
    }

    private LocalDateTime processingCutoff() {
        return ClinicTime.now().minusSeconds(processingTimeoutSeconds);
    }

    private String trimErrorMessage(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return "Không thể phát hành phiếu thu/hóa đơn điện tử.";
        }

        return errorMessage.length() <= 2000
                ? errorMessage
                : errorMessage.substring(0, 2000);
    }

    private TransactionTemplate requiresNewTransactionTemplate() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transactionTemplate;
    }

    private <T> T runInRequiresNewTransaction(TransactionCallback<T> callback) {
        return requiresNewTransactionTemplate().execute(status -> callback.execute());
    }

    @FunctionalInterface
    private interface TransactionCallback<T> {
        T execute();
    }
}
