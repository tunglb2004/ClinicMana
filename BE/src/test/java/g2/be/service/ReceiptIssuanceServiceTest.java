package g2.be.service;

import g2.be.dto.response.ReceiptResponse;
import g2.be.entity.Appointment;
import g2.be.entity.Invoice;
import g2.be.entity.MedicalRecord;
import g2.be.entity.Patient;
import g2.be.entity.Service;
import g2.be.entity.ServiceOrder;
import g2.be.entity.User;
import g2.be.exception.AppException;
import g2.be.exception.ErrorCode;
import g2.be.repository.InvoiceRepository;
import g2.be.repository.ServiceOrderRepository;
import g2.be.util.ClinicTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReceiptIssuanceServiceTest {

    private static final DateTimeFormatter RECEIPT_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private ServiceOrderRepository serviceOrderRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private SepayEinvoiceClient sepayEinvoiceClient;

    @Mock
    private PlatformTransactionManager transactionManager;

    private ReceiptIssuanceService receiptIssuanceService;

    @BeforeEach
    void setUp() {
        receiptIssuanceService = new ReceiptIssuanceService(
                invoiceRepository,
                serviceOrderRepository,
                emailService,
                sepayEinvoiceClient,
                transactionManager
        );
        ReflectionTestUtils.setField(receiptIssuanceService, "batchSize", 5);
        ReflectionTestUtils.setField(receiptIssuanceService, "processingTimeoutSeconds", 300L);

        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    }

    @Test
    @DisplayName("Issue receipt now for a paid invoice")
    void issueReceiptNow_PaidInvoice_IssuesReceipt() {
        MedicalRecord medicalRecord = buildCompletedMedicalRecord("MR-001", "APT-001");
        Invoice invoice = buildPaidInvoice("INV-001", medicalRecord, "NONE", new BigDecimal("150000"));
        ServiceOrder paidOrder = buildServiceOrder(
                "ORD-001",
                medicalRecord,
                invoice.getInvoiceId(),
                "INITIAL",
                "PAID",
                buildService("SV-001", "General exam", new BigDecimal("150000")));

        when(invoiceRepository.findById("INV-001")).thenReturn(Optional.of(invoice));
        when(invoiceRepository.findByMedicalRecord_RecordId("MR-001")).thenReturn(List.of(invoice));
        when(invoiceRepository.claimReceiptIssuanceByRecord(
                anyString(), anyString(), anyString(), anyString(), anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(1);
        when(invoiceRepository.findTopByReceiptNumberStartingWithOrderByReceiptNumberDesc(anyString()))
                .thenReturn(Optional.empty());
        when(invoiceRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(serviceOrderRepository.findByMedicalRecord_RecordId("MR-001")).thenReturn(List.of(paidOrder));
        when(sepayEinvoiceClient.issueInvoice(any(Invoice.class), eq(List.of(paidOrder)), any(LocalDateTime.class)))
                .thenReturn(new SepayEinvoiceClient.IssuedEinvoice(
                        "REF-001",
                        "https://einvoice-api-sandbox.sepay.vn/pdf/REF-001",
                        null,
                        "TRACK-001"
                ));

        ReceiptResponse response = receiptIssuanceService.issueReceiptNow("INV-001");

        assertNotNull(response);
        assertEquals("ISSUED", response.getReceiptStatus());
        assertEquals("REF-001", response.getEInvoiceCode());
        assertEquals("https://einvoice-api-sandbox.sepay.vn/pdf/REF-001", response.getEInvoiceUrl());
        assertNotNull(response.getReceiptNumber());
        assertNotNull(response.getReceiptIssuedAt());
        assertEquals(new BigDecimal("150000"), response.getCustomerPaid());
        assertNull(invoice.getReceiptProcessingStartedAt());
        assertNull(invoice.getReceiptErrorMessage());
        verify(emailService).sendReceiptIssuedEmail(
                eq("patient@example.com"),
                eq("Nguyen Van A"),
                eq("INV-001"),
                eq(response.getEInvoiceCode()),
                eq(response.getEInvoiceUrl()),
                eq(response.getReceiptIssuedAt()),
                eq(response.getCustomerPaid())
        );
    }

    @Test
    @DisplayName("Use highest existing receipt suffix plus one")
    void issueReceiptNow_UsesHighestExistingReceiptSuffix() {
        String todayPrefix = "PT-" + ClinicTime.today().format(RECEIPT_DATE_FORMATTER);

        MedicalRecord medicalRecord = buildCompletedMedicalRecord("MR-003", "APT-003");
        Invoice invoice = buildPaidInvoice("INV-003", medicalRecord, "NONE", new BigDecimal("90000"));
        ServiceOrder paidOrder = buildServiceOrder(
                "ORD-003",
                medicalRecord,
                invoice.getInvoiceId(),
                "INITIAL",
                "PAID",
                buildService("SV-003", "Follow-up exam", new BigDecimal("90000")));

        when(invoiceRepository.findById("INV-003")).thenReturn(Optional.of(invoice));
        when(invoiceRepository.findByMedicalRecord_RecordId("MR-003")).thenReturn(List.of(invoice));
        when(invoiceRepository.claimReceiptIssuanceByRecord(
                anyString(), anyString(), anyString(), anyString(), anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(1);
        when(invoiceRepository.findTopByReceiptNumberStartingWithOrderByReceiptNumberDesc(todayPrefix))
                .thenReturn(Optional.of(Invoice.builder().receiptNumber(todayPrefix + "-0007").build()));
        when(invoiceRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(serviceOrderRepository.findByMedicalRecord_RecordId("MR-003")).thenReturn(List.of(paidOrder));
        when(sepayEinvoiceClient.issueInvoice(any(Invoice.class), eq(List.of(paidOrder)), any(LocalDateTime.class)))
                .thenReturn(new SepayEinvoiceClient.IssuedEinvoice(
                        "REF-003",
                        "https://einvoice-api-sandbox.sepay.vn/pdf/REF-003",
                        null,
                        "TRACK-003"
                ));

        ReceiptResponse response = receiptIssuanceService.issueReceiptNow("INV-003");

        assertEquals(todayPrefix + "-0008", response.getReceiptNumber());
    }

    @Test
    @DisplayName("Async scheduler marks the record as failed on provider error")
    void processCompletedAppointmentReceipts_OnUnexpectedFailure_MarksInvoiceFailed() {
        MedicalRecord medicalRecord = buildCompletedMedicalRecord("MR-002", "APT-002");
        Invoice invoice = buildPaidInvoice("INV-002", medicalRecord, "NONE", new BigDecimal("110000"));
        ServiceOrder paidOrder = buildServiceOrder(
                "ORD-002",
                medicalRecord,
                invoice.getInvoiceId(),
                "INITIAL",
                "PAID",
                buildService("SV-002", "Lab test", new BigDecimal("110000")));

        when(invoiceRepository.findRecordIdsEligibleForReceiptIssuance(
                anyString(), anyString(), anyCollection(), anyString(), any(LocalDateTime.class), any()))
                .thenReturn(List.of("MR-002"));
        when(invoiceRepository.findByMedicalRecord_RecordId("MR-002")).thenReturn(List.of(invoice), List.of(invoice));
        when(invoiceRepository.claimReceiptIssuanceByRecord(
                anyString(), anyString(), anyString(), anyString(), anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(1);
        when(serviceOrderRepository.findByMedicalRecord_RecordId("MR-002")).thenReturn(List.of(paidOrder));
        when(sepayEinvoiceClient.issueInvoice(any(Invoice.class), eq(List.of(paidOrder)), any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("provider timeout"));

        receiptIssuanceService.processCompletedAppointmentReceipts();

        verify(invoiceRepository).markReceiptIssuanceFailed("INV-002", "FAILED", "ISSUED", "provider timeout");
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Async scheduler consolidates multiple paid invoices into one e-invoice")
    void processCompletedAppointmentReceipts_MultiplePaidInvoices_IssuesSingleConsolidatedReceipt() {
        MedicalRecord medicalRecord = buildCompletedMedicalRecord("MR-006", "APT-006");
        Invoice initialInvoice = buildPaidInvoice("INV-006-A", medicalRecord, "NONE", new BigDecimal("100000"));
        Invoice supplementInvoice = buildPaidInvoice("INV-006-B", medicalRecord, "NONE", new BigDecimal("50000"));

        ServiceOrder initialOrder = buildServiceOrder(
                "ORD-006-A",
                medicalRecord,
                initialInvoice.getInvoiceId(),
                "INITIAL",
                "PAID",
                buildService("SV-006-A", "General checkup", new BigDecimal("100000")));
        ServiceOrder supplementOrder = buildServiceOrder(
                "ORD-006-B",
                medicalRecord,
                supplementInvoice.getInvoiceId(),
                "SUPPLEMENT",
                "PAID",
                buildService("SV-006-B", "Extra ultrasound", new BigDecimal("50000")));

        when(invoiceRepository.findRecordIdsEligibleForReceiptIssuance(
                anyString(), anyString(), anyCollection(), anyString(), any(LocalDateTime.class), any()))
                .thenReturn(List.of("MR-006"));
        when(invoiceRepository.findByMedicalRecord_RecordId("MR-006"))
                .thenReturn(List.of(initialInvoice, supplementInvoice), List.of(initialInvoice, supplementInvoice));
        when(invoiceRepository.claimReceiptIssuanceByRecord(
                anyString(), anyString(), anyString(), anyString(), anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(2);
        when(invoiceRepository.findTopByReceiptNumberStartingWithOrderByReceiptNumberDesc(anyString()))
                .thenReturn(Optional.empty());
        when(invoiceRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(serviceOrderRepository.findByMedicalRecord_RecordId("MR-006"))
                .thenReturn(List.of(initialOrder, supplementOrder));
        when(sepayEinvoiceClient.issueInvoice(any(Invoice.class), anyList(), any(LocalDateTime.class)))
                .thenReturn(new SepayEinvoiceClient.IssuedEinvoice(
                        "REF-006",
                        "https://einvoice-api-sandbox.sepay.vn/pdf/REF-006",
                        null,
                        "TRACK-006"
                ));

        receiptIssuanceService.processCompletedAppointmentReceipts();

        assertEquals("ISSUED", initialInvoice.getReceiptStatus());
        assertEquals("ISSUED", supplementInvoice.getReceiptStatus());
        assertEquals("REF-006", initialInvoice.getEInvoiceCode());
        assertEquals("REF-006", supplementInvoice.getEInvoiceCode());
        verify(sepayEinvoiceClient).issueInvoice(any(Invoice.class), anyList(), any(LocalDateTime.class));
        verify(emailService).sendReceiptIssuedEmail(
                eq("patient@example.com"),
                eq("Nguyen Van A"),
                eq("INV-006-A"),
                eq("REF-006"),
                eq("https://einvoice-api-sandbox.sepay.vn/pdf/REF-006"),
                any(LocalDateTime.class),
                eq(new BigDecimal("150000"))
        );
    }

    @Test
    @DisplayName("Only include orders covered by paid invoices in the same record")
    void issueReceiptNow_UsesOnlyPaidInvoiceOrdersAcrossRecord() {
        MedicalRecord medicalRecord = buildCompletedMedicalRecord("MR-004", "APT-004");
        Invoice initialInvoice = buildPaidInvoice("INV-004", medicalRecord, "NONE", new BigDecimal("120000"));
        Invoice supplementInvoice = buildPaidInvoice("INV-004-S1", medicalRecord, "NONE", new BigDecimal("80000"));
        Invoice pendingSupplementInvoice = Invoice.builder()
                .invoiceId("INV-004-S2")
                .medicalRecord(medicalRecord)
                .paymentStatus("PENDING")
                .receiptStatus("NONE")
                .build();

        ServiceOrder initialOrder = buildServiceOrder(
                "ORD-004-A",
                medicalRecord,
                initialInvoice.getInvoiceId(),
                "INITIAL",
                "PAID",
                buildService("SV-004-A", "Initial exam", new BigDecimal("120000")));
        ServiceOrder paidSupplementOrder = buildServiceOrder(
                "ORD-004-B",
                medicalRecord,
                supplementInvoice.getInvoiceId(),
                "SUPPLEMENT",
                "PAID",
                buildService("SV-004-B", "Ultrasound", new BigDecimal("80000")));
        ServiceOrder pendingSupplementOrder = buildServiceOrder(
                "ORD-004-C",
                medicalRecord,
                pendingSupplementInvoice.getInvoiceId(),
                "SUPPLEMENT",
                "PENDING",
                buildService("SV-004-C", "Extra lab test", new BigDecimal("50000")));

        when(invoiceRepository.findById("INV-004")).thenReturn(Optional.of(initialInvoice));
        when(invoiceRepository.findByMedicalRecord_RecordId("MR-004"))
                .thenReturn(List.of(initialInvoice, supplementInvoice, pendingSupplementInvoice));
        when(invoiceRepository.claimReceiptIssuanceByRecord(
                anyString(), anyString(), anyString(), anyString(), anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(1);
        when(invoiceRepository.findTopByReceiptNumberStartingWithOrderByReceiptNumberDesc(anyString()))
                .thenReturn(Optional.empty());
        when(invoiceRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(serviceOrderRepository.findByMedicalRecord_RecordId("MR-004"))
                .thenReturn(List.of(initialOrder, paidSupplementOrder, pendingSupplementOrder));
        when(sepayEinvoiceClient.issueInvoice(any(Invoice.class), anyList(), any(LocalDateTime.class)))
                .thenReturn(new SepayEinvoiceClient.IssuedEinvoice(
                        "REF-004",
                        "https://einvoice-api-sandbox.sepay.vn/pdf/REF-004",
                        null,
                        "TRACK-004"
                ));

        receiptIssuanceService.issueReceiptNow("INV-004");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ServiceOrder>> orderCaptor = ArgumentCaptor.forClass(List.class);
        verify(sepayEinvoiceClient).issueInvoice(any(Invoice.class), orderCaptor.capture(), any(LocalDateTime.class));
        assertEquals(
                List.of("ORD-004-A", "ORD-004-B"),
                orderCaptor.getValue().stream().map(ServiceOrder::getOrderId).toList());
    }

    @Test
    @DisplayName("Do not issue again when another invoice in the record is already issued")
    void issueReceiptNow_RecordAlreadyIssued_ThrowsAlreadyIssued() {
        MedicalRecord medicalRecord = buildCompletedMedicalRecord("MR-005", "APT-005");
        Invoice issuedInvoice = buildPaidInvoice("INV-005-A", medicalRecord, "ISSUED", new BigDecimal("100000"));
        Invoice currentInvoice = buildPaidInvoice("INV-005-B", medicalRecord, "NONE", new BigDecimal("50000"));

        when(invoiceRepository.findById("INV-005-B")).thenReturn(Optional.of(currentInvoice));
        when(invoiceRepository.findByMedicalRecord_RecordId("MR-005"))
                .thenReturn(List.of(issuedInvoice, currentInvoice));

        AppException exception = assertThrows(AppException.class,
                () -> receiptIssuanceService.issueReceiptNow("INV-005-B"));

        assertEquals(ErrorCode.RECEIPT_ALREADY_ISSUED, exception.getErrorCode());
        verify(invoiceRepository, never()).claimReceiptIssuanceByRecord(
                anyString(), anyString(), anyString(), anyString(), anyString(), any(LocalDateTime.class), any(LocalDateTime.class));
        verifyNoInteractions(sepayEinvoiceClient, emailService);
    }

    private MedicalRecord buildCompletedMedicalRecord(String recordId, String appointmentId) {
        User user = User.builder()
                .email("patient@example.com")
                .fullName("Nguyen Van A")
                .build();
        Patient patient = Patient.builder()
                .patientId("PAT-001")
                .user(user)
                .build();
        Appointment appointment = Appointment.builder()
                .appointmentId(appointmentId)
                .patient(patient)
                .status("COMPLETED")
                .build();

        return MedicalRecord.builder()
                .recordId(recordId)
                .appointment(appointment)
                .patient(patient)
                .status("COMPLETED")
                .build();
    }

    private Invoice buildPaidInvoice(String invoiceId,
                                     MedicalRecord medicalRecord,
                                     String receiptStatus,
                                     BigDecimal customerPaid) {
        return Invoice.builder()
                .invoiceId(invoiceId)
                .medicalRecord(medicalRecord)
                .paymentStatus("PAID")
                .paymentMethod("CASH")
                .totalServiceFee(customerPaid)
                .customerPaid(customerPaid)
                .receiptStatus(receiptStatus)
                .build();
    }

    private ServiceOrder buildServiceOrder(String orderId,
                                           MedicalRecord medicalRecord,
                                           String invoiceId,
                                           String orderType,
                                           String status,
                                           Service service) {
        return ServiceOrder.builder()
                .orderId(orderId)
                .medicalRecord(medicalRecord)
                .invoiceId(invoiceId)
                .orderType(orderType)
                .status(status)
                .service(service)
                .build();
    }

    private Service buildService(String serviceId, String serviceName, BigDecimal price) {
        return Service.builder()
                .serviceId(serviceId)
                .serviceName(serviceName)
                .price(price)
                .build();
    }
}
