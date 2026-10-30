package g2.be.service;

import g2.be.dto.request.PrepaymentRequest;
import g2.be.dto.response.InsuranceBreakdownResponse;
import g2.be.entity.Appointment;
import g2.be.entity.Department;
import g2.be.entity.Doctor;
import g2.be.entity.Invoice;
import g2.be.entity.MedicalRecord;
import g2.be.entity.Room;
import g2.be.entity.Service;
import g2.be.exception.AppException;
import g2.be.exception.ErrorCode;
import g2.be.repository.AppointmentRepository;
import g2.be.repository.InvoiceRepository;
import g2.be.repository.MedicalRecordRepository;
import g2.be.util.ClinicTime;
import g2.be.util.DoctorDepartmentClassifier;
import g2.be.util.QueuePriorityRules;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

        private final AppointmentRepository appointmentRepository;
        private final MedicalRecordRepository medicalRecordRepository;
        private final InvoiceRepository invoiceRepository;
        private final g2.be.repository.ServiceOrderRepository serviceOrderRepository;
        private final CitizenInsuranceService citizenInsuranceService;
        private final g2.be.repository.RoomRepository roomRepository;
        private final OnDutyRoomAssignmentResolver onDutyRoomAssignmentResolver;
        @org.springframework.beans.factory.annotation.Value("${sepay.api-key}")
        private String sepayApiKey;

        @org.springframework.beans.factory.annotation.Value("${sepay.bank}")
        private String sepayBank;

        @org.springframework.beans.factory.annotation.Value("${sepay.acc}")
        private String sepayAcc;

        @Transactional
        public g2.be.dto.response.PaymentResponse collectPrepayment(PrepaymentRequest request) {
                // Check if already paid
                if (invoiceRepository.existsByMedicalRecord_Appointment_AppointmentIdAndPaymentStatus(
                                request.getAppointmentId(), "PAID")) {
                        throw new AppException(ErrorCode.PAYMENT_ALREADY_PAID);
                }

                Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                                .orElseThrow(() -> new AppException(ErrorCode.APPOINTMENT_NOT_FOUND));
                ensureServicesAssignableAtAppointment(appointment, appointment.getServices(), "lịch hẹn");

                // Calculate total from services
                BigDecimal totalServiceFee = appointment.getServices().stream()
                                .map(g2.be.entity.Service::getPrice)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Apply insurance if cccdNumber provided
                BigDecimal insurancePaid = BigDecimal.ZERO;
                BigDecimal customerPaid = totalServiceFee;
                InsuranceBreakdownResponse insuranceBreakdown = null;

                if (request.getCccdNumber() != null && !request.getCccdNumber().isBlank()) {
                        try {
                                insuranceBreakdown = citizenInsuranceService
                                                .calculateInsuranceForServices(request.getCccdNumber(), appointment.getServices());
                                insurancePaid = insuranceBreakdown.getTotalInsurancePaid();
                                customerPaid = insuranceBreakdown.getTotalPatientPaid();
                                log.info("Prepayment: BHYT applied for CCCD {}. Insurance: {}, Patient: {}",
                                                request.getCccdNumber(), insurancePaid, customerPaid);
                        } catch (AppException e) {
                                log.warn("Prepayment: Could not apply insurance for CCCD {}: {}",
                                                request.getCccdNumber(), e.getMessage());
                                // Không có BHYT hợp lệ → bệnh nhân trả full
                        }
                }

                // Determine Payment Status
                String paymentStatus;
                String message;
                String paymentUrl = null;

                if ("CASH".equalsIgnoreCase(request.getPaymentMethod())) {
                        paymentStatus = "PAID";
                        message = "Thanh toán tiền mặt thành công.";
                } else {
                        // QR, TRANSFER
                        paymentStatus = "PENDING";
                        message = "Vui lòng quét mã QR để thanh toán.";
                        String invoiceRef = "INV" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                        paymentUrl = String.format("https://qr.sepay.vn/img?acc=%s&bank=%s&amount=%s&des=%s",
                                        sepayAcc, sepayBank, customerPaid, invoiceRef);
                }

                // Check if MedicalRecord exists, if not create one
                MedicalRecord medicalRecord = medicalRecordRepository.findByAppointment(appointment)
                                .orElse(null);

                if (medicalRecord == null) {
                        medicalRecord = buildMedicalRecordForAppointment(appointment, "WAITING");
                        medicalRecord = medicalRecordRepository.save(medicalRecord);

                }

                // Create Invoice
                String invoiceId;
                if (paymentUrl != null && paymentUrl.contains("des=INV")) {
                        invoiceId = paymentUrl.substring(paymentUrl.indexOf("des=") + 4);
                } else {
                        invoiceId = "INV" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                }

                // Get current logged in user (Cashier)
                String cashierId = null;
                try {
                        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                                        .getContext().getAuthentication();
                        if (authentication != null
                                        && authentication.getPrincipal() instanceof g2.be.security.UserDetailsImpl) {
                                g2.be.security.UserDetailsImpl userDetails = (g2.be.security.UserDetailsImpl) authentication
                                                .getPrincipal();
                                cashierId = String.valueOf(userDetails.getId());
                        }
                } catch (Exception e) {
                        log.warn("Could not get current user for cashierId", e);
                }

                Invoice invoice = Invoice.builder()
                                .invoiceId(invoiceId)
                                .medicalRecord(medicalRecord)
                                .cashierId(cashierId)
                                .totalServiceFee(totalServiceFee)
                                .insurancePaid(insurancePaid)
                                .customerPaid(customerPaid)
                                .paymentStatus(paymentStatus)
                                .paidAt("PAID".equals(paymentStatus) ? ClinicTime.now() : null)
                                .paymentMethod(request.getPaymentMethod())
                                .build();

                invoiceRepository.save(invoice);

                if ("PAID".equals(paymentStatus)) {
                        autoAssignDoctor(appointment, true);
                        assignQueueNumberIfMissing(appointment);

                        if (medicalRecord.getDoctor() != appointment.getDoctor()) {
                                medicalRecord.setDoctor(appointment.getDoctor());
                                medicalRecordRepository.save(medicalRecord);
                        }
                }

                // Create ServiceOrder for each service in the appointment
                java.util.List<g2.be.entity.ServiceOrder> createdOrders = new java.util.ArrayList<>();
                for (g2.be.entity.Service service : appointment.getServices()) {
                        Room assignedServiceRoom = resolveOfficialRoomForService(service, appointment);
                        g2.be.entity.ServiceOrder serviceOrder = g2.be.entity.ServiceOrder.builder()
                                        .orderId("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                                        .medicalRecord(medicalRecord)
                                        .service(service)
                                        .assignedRoom(assignedServiceRoom)
                                        .technicianId(resolveServiceTechnicianId(service, appointment, assignedServiceRoom))
                                        .status(paymentStatus)
                                        .orderType("INITIAL")
                                        .invoiceId(invoiceId)
                                        .build();
                        createdOrders.add(serviceOrderRepository.save(serviceOrder));
                }

                // Update appointment status ONLY if CASH (PAID)
                if ("PAID".equals(paymentStatus)) {
                        appointment.setStatus("WAITING");
                        // Priority Queue: khám mới → score 0, bắt đầu chờ từ bây giờ
                        appointment.setPriorityScore(QueuePriorityRules.basePriorityForVisitType(appointment.getVisitType()));
                        appointment.setWaitingSince(ClinicTime.now());
                        appointmentRepository.save(appointment);

                        // Tăng currentQueueLength của phòng
                        incrementQueueLengthForOfficialRooms(appointment, createdOrders);
                }

                // Update paymentUrl with real invoiceId if it was pending
                if ("PENDING".equals(paymentStatus)) {
                        paymentUrl = String.format("https://qr.sepay.vn/img?acc=%s&bank=%s&amount=%s&des=%s",
                                        sepayAcc, sepayBank, customerPaid, invoiceId);
                }

                return g2.be.dto.response.PaymentResponse.builder()
                                .invoiceId(invoiceId)
                                .paymentStatus(paymentStatus)
                                .amount(customerPaid)
                                .totalServiceFee(totalServiceFee)
                                .insurancePaid(insurancePaid)
                                .customerPaid(customerPaid)
                                .paymentUrl(paymentUrl)
                                .insuranceBreakdown(insuranceBreakdown)
                                .message(message)
                                .build();
        }

        private MedicalRecord buildMedicalRecordForAppointment(Appointment appointment, String status) {
                MedicalRecord parentRecord = appointment.getFollowUpSourceRecord();
                MedicalRecord rootRecord = parentRecord == null
                                ? null
                                : parentRecord.getRootRecord() != null ? parentRecord.getRootRecord() : parentRecord;

                return MedicalRecord.builder()
                                .recordId("REC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                                .appointment(appointment)
                                .patient(appointment.getPatient())
                                .doctor(appointment.getDoctor())
                                .status(status)
                                .visitType("FOLLOW_UP".equalsIgnoreCase(appointment.getVisitType()) ? "FOLLOW_UP" : "INITIAL")
                                .parentRecord(parentRecord)
                                .rootRecord(rootRecord)
                                .build();
        }

        @Transactional
        public void processSepayWebhook(java.util.Map<String, Object> payload, String authorizationHeader) {
                log.info("=== SePay Webhook Processing ===");
                log.info("Payload: {}", payload);

                // 1. Validate API Key
                if (authorizationHeader == null || !authorizationHeader.startsWith("Apikey ")) {
                        log.warn("Invalid Authorization header format: {}", authorizationHeader);
                        throw new AppException(ErrorCode.UNAUTHENTICATED); // Or custom error
                }
                String apiKey = authorizationHeader.substring("Apikey ".length());
                if (!sepayApiKey.equals(apiKey)) {
                        log.warn("Unauthorized SePay Webhook: Invalid API Key");
                        throw new AppException(ErrorCode.UNAUTHENTICATED);
                }

                // 2. Extract Invoice ID and Amount
                // Content usually contains the invoiceId e.g. "INV-12345678"
                String content = firstNonBlankPayloadValue(
                                payload,
                                "content",
                                "description",
                                "transactionContent",
                                "transferContent",
                                "remark");
                BigDecimal receivedAmount = extractTransferAmount(payload);

                if (content == null || receivedAmount == null) {
                        log.warn("Missing content or transferAmount in payload");
                        throw new AppException(ErrorCode.INVALID_KEY); // Reusing INVALID_KEY as Invalid Request
                }

                // Simple regex or contains check for Invoice ID
                // Assuming Invoice ID format "INV-" followed by 8 chars
                String invoiceId = extractInvoiceId(content);
                if (invoiceId == null) {
                        log.warn("Could not find valid Invoice ID in content: {}", content);
                        return; // Or throw error, but we might want to return 200 to SePay to stop retries if
                                // it's junk data
                }

                log.info("Processing Payment for Invoice: {}", invoiceId);

                Invoice invoice = invoiceRepository.findById(invoiceId)
                                .orElseThrow(() -> new AppException(ErrorCode.INVOICE_NOT_FOUND)); // Need to ensure
                                                                                                   // INVOICE_NOT_FOUND
                                                                                                   // exists or use
                                                                                                   // another

                if ("PAID".equals(invoice.getPaymentStatus())) {
                        log.info("Invoice {} is already PAID", invoiceId);
                        return;
                }

                if ("CANCELLED".equals(invoice.getPaymentStatus())) {
                        log.info("Invoice {} was cancelled before payment confirmation. Ignoring webhook.", invoiceId);
                        return;
                }

                // 3. Verify Amount
                BigDecimal expectedAmount = expectedPayableAmount(invoice);

                // Compare with some tolerance if needed, but usually exact for automated
                // systems
                if (receivedAmount.compareTo(expectedAmount) < 0) {
                        log.warn("Insufficient payment for Invoice {}. Expected: {}, Received: {}", invoiceId,
                                        expectedAmount, receivedAmount);
                        // Logic for partial payment could go here. For now, we reject or just log.
                        // We might technically update customerPaid but not set status to PAID.
                        return;
                }

                java.util.List<g2.be.entity.ServiceOrder> orders = serviceOrderRepository
                                .findByInvoiceId(invoiceId);
                boolean supplementInvoice = orders.stream()
                                .anyMatch(order -> "SUPPLEMENT".equalsIgnoreCase(order.getOrderType()));
                Appointment appointment = invoice.getMedicalRecord() != null
                                ? invoice.getMedicalRecord().getAppointment()
                                : null;
                List<Service> payableServices = supplementInvoice
                                ? orders.stream()
                                                .map(g2.be.entity.ServiceOrder::getService)
                                                .collect(Collectors.toList())
                                : appointment != null ? appointment.getServices() : List.of();
                ensureServicesAssignableAtAppointment(appointment, payableServices, "hóa đơn");

                // 4. Update Statuses
                invoice.setPaymentStatus("PAID");
                invoice.setCustomerPaid(expectedAmount);
                invoice.setPaymentMethod("QR_SEPAY"); // Or keep original if it was "QR"

                // Save transaction details
                Object txnIdObj = payload.get("id");
                if (txnIdObj != null) {
                        invoice.setTransactionId(txnIdObj.toString());
                }
                invoice.setPaidAt(ClinicTime.now());

                // Save gateway payload for audit trail
                try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        invoice.setGatewayPayload(mapper.writeValueAsString(payload));
                } catch (Exception e) {
                        log.warn("Could not serialize gateway payload: {}", e.getMessage());
                        invoice.setGatewayPayload(payload.toString());
                }

                invoiceRepository.save(invoice);

                if (invoice.getMedicalRecord() != null) {
                        if (appointment != null && !supplementInvoice) {
                                // Load Balancing: tự động gán bác sĩ/phòng tối ưu
                                autoAssignDoctor(appointment, true);
                                assignQueueNumberIfMissing(appointment);

                                if (invoice.getMedicalRecord().getDoctor() != appointment.getDoctor()) {
                                        invoice.getMedicalRecord().setDoctor(appointment.getDoctor());
                                        medicalRecordRepository.save(invoice.getMedicalRecord());
                                }

                                appointment.setStatus("WAITING");
                                // Priority Queue: xác định bệnh nhân quay lại hay mới
                                // Nếu có ServiceOrder PAID trước đó (ngoài batch này) → quay lại
                                long previousPaidCount = serviceOrderRepository
                                        .findByMedicalRecord_RecordId(
                                                invoice.getMedicalRecord().getRecordId())
                                        .stream()
                                        .filter(o -> "PAID".equals(o.getStatus())
                                                && !invoiceId.equals(o.getInvoiceId()))
                                        .count();
                                appointment.setPriorityScore(!"FOLLOW_UP".equalsIgnoreCase(appointment.getVisitType())
                                                && previousPaidCount > 0
                                                ? QueuePriorityRules.RETURNING_FROM_PARACLINICAL
                                                : QueuePriorityRules.basePriorityForVisitType(appointment.getVisitType()));
                                appointment.setWaitingSince(ClinicTime.now());
                                appointmentRepository.save(appointment);

                                // Tăng currentQueueLength của phòng
                                // Queue length is incremented after service orders get their official rooms.
                        }
                }

                for (g2.be.entity.ServiceOrder order : orders) {
                        if ("PENDING".equals(order.getStatus())) {
                                order.setStatus("PAID");
                        }
                        Room assignedServiceRoom = resolveOfficialRoomForService(order.getService(), appointment);
                        order.setAssignedRoom(assignedServiceRoom);
                        order.setTechnicianId(resolveServiceTechnicianId(order.getService(), appointment, assignedServiceRoom));
                        serviceOrderRepository.save(order);
                }

                if (appointment != null) {
                        incrementQueueLengthForOfficialRooms(supplementInvoice ? null : appointment, orders);
                }

                log.info("Successfully processed payment for Invoice {}", invoiceId);
        }

        @Transactional
        public g2.be.dto.response.PaymentResponse collectSupplementPayment(
                        g2.be.dto.request.SupplementPaymentRequest request) {

                MedicalRecord medicalRecord = medicalRecordRepository.findById(request.getRecordId())
                                .orElseThrow(() -> new AppException(ErrorCode.RECORD_NOT_FOUND));
                ensureRecordOpenForSupplement(medicalRecord);

                // Supplement payment must only cover orders created in the supplement batch.
                java.util.List<g2.be.entity.ServiceOrder> allPendingOrders = serviceOrderRepository
                                .findByMedicalRecord_RecordIdAndOrderTypeAndStatus(
                                                request.getRecordId(), "SUPPLEMENT", "PENDING");

                if (allPendingOrders.isEmpty()) {
                        throw new AppException(ErrorCode.NO_SUPPLEMENT_ORDERS);
                }

                // Check if these orders already have a PENDING invoice (QR not yet confirmed)
                // If so → return existing invoice info instead of creating a duplicate
                String existingPendingInvoiceId = allPendingOrders.stream()
                                .map(g2.be.entity.ServiceOrder::getInvoiceId)
                                .filter(id -> id != null)
                                .findFirst()
                                .orElse(null);

                if (existingPendingInvoiceId != null) {
                        Invoice existingInvoice = invoiceRepository.findById(existingPendingInvoiceId).orElse(null);
                        if (existingInvoice != null && "PENDING".equals(existingInvoice.getPaymentStatus())) {
                                java.util.List<g2.be.entity.ServiceOrder> existingPendingOrders = allPendingOrders.stream()
                                                .filter(order -> existingPendingInvoiceId.equals(order.getInvoiceId()))
                                                .collect(Collectors.toList());
                                List<Service> existingPendingServices = existingPendingOrders.stream()
                                                .map(g2.be.entity.ServiceOrder::getService)
                                                .collect(Collectors.toList());
                                ensureServicesAssignableAtAppointment(
                                                medicalRecord.getAppointment(),
                                                existingPendingServices,
                                                "dịch vụ bổ sung");

                                if ("CASH".equalsIgnoreCase(request.getPaymentMethod())) {
                                        InsuranceBreakdownResponse insuranceBreakdown = null;
                                        if (request.getCccdNumber() != null && !request.getCccdNumber().isBlank()) {
                                                try {
                                                        insuranceBreakdown = citizenInsuranceService
                                                                        .calculateInsuranceForServices(
                                                                                        request.getCccdNumber(),
                                                                                        existingPendingServices);
                                                        existingInvoice.setTotalServiceFee(insuranceBreakdown.getTotalClinicPrice());
                                                        existingInvoice.setInsurancePaid(insuranceBreakdown.getTotalInsurancePaid());
                                                        existingInvoice.setCustomerPaid(insuranceBreakdown.getTotalPatientPaid());
                                                } catch (AppException e) {
                                                        log.warn("Supplement pending invoice {}: Could not apply insurance for CCCD {}: {}",
                                                                        existingPendingInvoiceId,
                                                                        request.getCccdNumber(),
                                                                        e.getMessage());
                                                }
                                        }

                                        existingInvoice.setPaymentStatus("PAID");
                                        existingInvoice.setPaymentMethod("CASH");
                                        existingInvoice.setPaidAt(ClinicTime.now());
                                        String cashierId = resolveCurrentCashierId();
                                        if (cashierId != null) {
                                                existingInvoice.setCashierId(cashierId);
                                        }
                                        invoiceRepository.save(existingInvoice);

                                        markSupplementOrdersPaid(existingPendingOrders, medicalRecord.getAppointment());

                                        return g2.be.dto.response.PaymentResponse.builder()
                                                        .invoiceId(existingPendingInvoiceId)
                                                        .paymentStatus("PAID")
                                                        .amount(existingInvoice.getCustomerPaid())
                                                        .totalServiceFee(existingInvoice.getTotalServiceFee())
                                                        .insurancePaid(existingInvoice.getInsurancePaid())
                                                        .customerPaid(existingInvoice.getCustomerPaid())
                                                        .insuranceBreakdown(insuranceBreakdown)
                                                        .message("Thanh toán bổ sung tiền mặt thành công.")
                                                        .build();
                                }
                                // Reuse existing PENDING invoice and rebuild QR URL.
                                String existingPaymentUrl = null;
                                if (!"CASH".equalsIgnoreCase(existingInvoice.getPaymentMethod())) {
                                        existingPaymentUrl = String.format(
                                                        "https://qr.sepay.vn/img?acc=%s&bank=%s&amount=%s&des=%s",
                                                        sepayAcc, sepayBank,
                                                        existingInvoice.getCustomerPaid(),
                                                        existingPendingInvoiceId);
                                }
                                log.info("Reusing existing PENDING invoice {} for record {}",
                                                existingPendingInvoiceId, request.getRecordId());
                                return g2.be.dto.response.PaymentResponse.builder()
                                                .invoiceId(existingPendingInvoiceId)
                                                .paymentStatus("PENDING")
                                                .amount(existingInvoice.getCustomerPaid())
                                                .totalServiceFee(existingInvoice.getTotalServiceFee())
                                                .insurancePaid(existingInvoice.getInsurancePaid())
                                                .customerPaid(existingInvoice.getCustomerPaid())
                                                .paymentUrl(existingPaymentUrl)
                                                .message("Hóa đơn QR đang chờ thanh toán. Vui lòng quét mã QR.")
                                                .build();
                        }
                }

                // Only process orders that don't yet have an invoice
                java.util.List<g2.be.entity.ServiceOrder> supplementOrders = allPendingOrders.stream()
                                .filter(o -> o.getInvoiceId() == null)
                                .collect(Collectors.toList());

                if (supplementOrders.isEmpty()) {
                        throw new AppException(ErrorCode.NO_SUPPLEMENT_ORDERS);
                }

                // Extract services from orders (needed for both reuse and new invoice paths)
                List<g2.be.entity.Service> services = supplementOrders.stream()
                                .map(g2.be.entity.ServiceOrder::getService)
                                .collect(Collectors.toList());
                Appointment supplementAppointment = medicalRecord.getAppointment();
                ensureServicesAssignableAtAppointment(supplementAppointment, services, "dịch vụ bổ sung");

                // Calculate totals — apply insurance if cccdNumber provided
                BigDecimal totalServiceFee = services.stream()
                                .map(g2.be.entity.Service::getPrice)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal insurancePaid = BigDecimal.ZERO;
                BigDecimal customerPaid = totalServiceFee;
                InsuranceBreakdownResponse insuranceBreakdown = null;

                if (request.getCccdNumber() != null && !request.getCccdNumber().isBlank()) {
                        try {
                                insuranceBreakdown = citizenInsuranceService
                                                .calculateInsuranceForServices(request.getCccdNumber(), services);
                                insurancePaid = insuranceBreakdown.getTotalInsurancePaid();
                                customerPaid = insuranceBreakdown.getTotalPatientPaid();
                                log.info("Supplement: BHYT applied for CCCD {}. Insurance: {}, Patient: {}",
                                                request.getCccdNumber(), insurancePaid, customerPaid);
                        } catch (AppException e) {
                                log.warn("Supplement: Could not apply insurance for CCCD {}: {}",
                                                request.getCccdNumber(), e.getMessage());
                                // Không có BHYT hợp lệ → bệnh nhân trả full
                        }
                }

                // Determine payment status
                String paymentStatus;
                String message;
                String paymentUrl = null;

                if ("CASH".equalsIgnoreCase(request.getPaymentMethod())) {
                        paymentStatus = "PAID";
                        message = "Thanh toán bổ sung tiền mặt thành công.";
                } else {
                        paymentStatus = "PENDING";
                        message = "Vui lòng quét mã QR để thanh toán bổ sung.";
                }

                // Create Invoice for supplement
                String invoiceId = "INV" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

                if (!"CASH".equalsIgnoreCase(request.getPaymentMethod())) {
                        paymentUrl = String.format("https://qr.sepay.vn/img?acc=%s&bank=%s&amount=%s&des=%s",
                                        sepayAcc, sepayBank, customerPaid, invoiceId);
                }

                // Get cashier ID
                String cashierId = null;
                try {
                        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                                        .getContext().getAuthentication();
                        if (authentication != null
                                        && authentication.getPrincipal() instanceof g2.be.security.UserDetailsImpl) {
                                g2.be.security.UserDetailsImpl userDetails = (g2.be.security.UserDetailsImpl) authentication
                                                .getPrincipal();
                                cashierId = String.valueOf(userDetails.getId());
                        }
                } catch (Exception e) {
                        log.warn("Could not get current user for cashierId", e);
                }

                Invoice invoice = Invoice.builder()
                                .invoiceId(invoiceId)
                                .medicalRecord(medicalRecord)
                                .cashierId(cashierId)
                                .totalServiceFee(totalServiceFee)
                                .insurancePaid(insurancePaid)
                                .customerPaid(customerPaid)
                                .paymentStatus(paymentStatus)
                                .paidAt("PAID".equals(paymentStatus) ? ClinicTime.now() : null)
                                .paymentMethod(request.getPaymentMethod())
                                .build();

                invoiceRepository.save(invoice);

                // Link supplement orders to this invoice and update status
                for (g2.be.entity.ServiceOrder order : supplementOrders) {
                        order.setInvoiceId(invoiceId);
                        order.setStatus(paymentStatus);
                        if ("PAID".equals(paymentStatus)) {
                                Room assignedServiceRoom = resolveOfficialRoomForService(order.getService(), supplementAppointment);
                                order.setAssignedRoom(assignedServiceRoom);
                                order.setTechnicianId(resolveServiceTechnicianId(order.getService(), supplementAppointment, assignedServiceRoom));
                        }
                        serviceOrderRepository.save(order);
                }

                // Increment room queue for each service (load-balanced room assignment)
                if ("PAID".equals(paymentStatus)) {
                        incrementQueueLengthForOfficialRooms(null, supplementOrders);
                }

                return g2.be.dto.response.PaymentResponse.builder()
                                .invoiceId(invoiceId)
                                .paymentStatus(paymentStatus)
                                .amount(customerPaid)
                                .totalServiceFee(totalServiceFee)
                                .insurancePaid(insurancePaid)
                                .customerPaid(customerPaid)
                                .paymentUrl(paymentUrl)
                                .insuranceBreakdown(insuranceBreakdown)
                                .message(message)
                                .build();
        }

        private void markSupplementOrdersPaid(
                        java.util.List<g2.be.entity.ServiceOrder> supplementOrders,
                        Appointment supplementAppointment) {
                if (supplementOrders == null || supplementOrders.isEmpty()) {
                        return;
                }

                for (g2.be.entity.ServiceOrder order : supplementOrders) {
                        order.setStatus("PAID");
                        Room assignedServiceRoom = resolveOfficialRoomForService(order.getService(), supplementAppointment);
                        order.setAssignedRoom(assignedServiceRoom);
                        order.setTechnicianId(resolveServiceTechnicianId(
                                        order.getService(),
                                        supplementAppointment,
                                        assignedServiceRoom));
                        serviceOrderRepository.save(order);
                }

                incrementQueueLengthForOfficialRooms(null, supplementOrders);
        }

        private String resolveCurrentCashierId() {
                try {
                        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                                        .getContext().getAuthentication();
                        if (authentication != null
                                        && authentication.getPrincipal() instanceof g2.be.security.UserDetailsImpl userDetails) {
                                return String.valueOf(userDetails.getId());
                        }
                } catch (Exception e) {
                        log.warn("Could not get current user for cashierId", e);
                }
                return null;
        }

        private void ensureRecordOpenForSupplement(MedicalRecord medicalRecord) {
                String medicalRecordStatus = medicalRecord.getStatus();
                String appointmentStatus = medicalRecord.getAppointment() != null
                                ? medicalRecord.getAppointment().getStatus()
                                : null;

                if ("COMPLETED".equals(medicalRecordStatus) || "COMPLETED".equals(appointmentStatus)) {
                        throw new AppException(ErrorCode.MEDICAL_RECORD_ALREADY_FINISHED);
                }
        }

        /**
         * Tìm phòng tối ưu cho một dịch vụ bổ sung dựa trên service_room mapping.
         * Chọn phòng ACTIVE có hàng đợi ngắn nhất.
         */
        private g2.be.entity.Room findOptimalRoomForService(g2.be.entity.Service service) {
                return findOptimalRoomForService(service, null);
        }

        private g2.be.entity.Room findOptimalRoomForService(g2.be.entity.Service service, Appointment appointment) {
                java.util.List<OnDutyRoomAssignmentResolver.RoomDoctorAssignment> assignments = appointment != null
                        ? onDutyRoomAssignmentResolver.findAssignmentsForServiceAt(
                                        service,
                                        appointment.getAppointmentDate(),
                                        appointment.getAppointmentTime())
                        : onDutyRoomAssignmentResolver.findCurrentAssignmentsForService(service, null, null);

                java.util.List<g2.be.entity.Room> candidateRooms = assignments.stream()
                                .map(OnDutyRoomAssignmentResolver.RoomDoctorAssignment::room)
                                .filter(Objects::nonNull)
                                .distinct()
                                .toList();
                if (candidateRooms.isEmpty()) {
                        return null;
                }

                return candidateRooms.stream()
                        .min(java.util.Comparator
                                .comparingInt(this::roomLoadScore)
                                .thenComparingInt(this::roomTypePriority)
                                .thenComparing(g2.be.entity.Room::getRoomId, java.util.Comparator.nullsLast(String::compareTo)))
                        .orElse(null);
        }

        private int roomLoadScore(Room room) {
                if (room == null || room.getRoomId() == null) {
                        return Integer.MAX_VALUE;
                }

                int baseAvgTime = room.getBaseAvgTime() != null ? room.getBaseAvgTime() : 15;
                int manualQueueLength = room.getCurrentQueueLength() != null ? room.getCurrentQueueLength() : 0;
                int liveAppointmentQueue = appointmentRepository
                                .findActiveQueueByRoomId(room.getRoomId(), ClinicTime.today(), ClinicTime.now())
                                .size();
                int liveServiceQueue = (int) Math.min(
                                Integer.MAX_VALUE,
                                serviceOrderRepository
                                                .findByRoomIdsAndDate(
                                                                java.util.List.of(room.getRoomId()),
                                                                ClinicTime.today(),
                                                                ClinicTime.now(),
                                                                "PAID",
                                                                PageRequest.of(0, 1))
                                                .getTotalElements());

                return Math.max(manualQueueLength, liveAppointmentQueue + liveServiceQueue) * baseAvgTime;
        }

        private String extractInvoiceId(String content) {
                if (content == null)
                        return null;
                // Regex to find INVXXXXXXXX (INV + 8 chars)
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("INV[A-Z0-9]{8}");
                java.util.regex.Matcher matcher = pattern.matcher(content);
                if (matcher.find()) {
                        return matcher.group();
                }
                return null;
        }

        private String firstNonBlankPayloadValue(java.util.Map<String, Object> payload, String... keys) {
                if (payload == null || keys == null) {
                        return null;
                }

                for (String key : keys) {
                        Object value = payload.get(key);
                        if (value != null && !value.toString().isBlank()) {
                                return value.toString();
                        }
                }
                return null;
        }

        private BigDecimal extractTransferAmount(java.util.Map<String, Object> payload) {
                if (payload == null) {
                        return null;
                }

                for (String key : List.of("transferAmount", "amount", "money", "totalAmount")) {
                        Object value = payload.get(key);
                        BigDecimal amount = parseAmount(value);
                        if (amount != null) {
                                return amount;
                        }
                }
                return null;
        }

        private BigDecimal parseAmount(Object value) {
                if (value == null) {
                        return null;
                }
                if (value instanceof BigDecimal bigDecimal) {
                        return bigDecimal;
                }
                if (value instanceof Number number) {
                        return BigDecimal.valueOf(number.doubleValue());
                }

                String normalized = value.toString()
                                .replace(",", "")
                                .replace(" ", "")
                                .trim();
                if (normalized.isBlank()) {
                        return null;
                }
                try {
                        return new BigDecimal(normalized);
                } catch (NumberFormatException ignored) {
                        return null;
                }
        }

        private BigDecimal expectedPayableAmount(Invoice invoice) {
                if (invoice == null) {
                        return BigDecimal.ZERO;
                }
                if (invoice.getCustomerPaid() != null && invoice.getCustomerPaid().compareTo(BigDecimal.ZERO) >= 0) {
                        return invoice.getCustomerPaid();
                }
                return invoice.getTotalServiceFee() != null ? invoice.getTotalServiceFee() : BigDecimal.ZERO;
        }

        private String resolveServiceTechnicianId(Service service, Appointment appointment, Room assignedRoom) {
                if (service == null
                                || service.getDepartment() == null
                                || service.getDepartment().getDeptType() != Department.DeptType.PARACLINICAL) {
                        return null;
                }

                if (assignedRoom == null) {
                        return null;
                }

                return findOnDutyAssignmentForRoom(service, assignedRoom, appointment)
                                .map(OnDutyRoomAssignmentResolver.RoomDoctorAssignment::doctor)
                                .map(Doctor::getDoctorId)
                                .orElse(null);
        }

        private void ensureServicesAssignableAtAppointment(
                        Appointment appointment,
                        List<Service> services,
                        String label) {
                if (appointment == null || services == null || services.isEmpty()) {
                        return;
                }

                List<String> missingServices = services.stream()
                                .filter(Objects::nonNull)
                                .filter(service -> service.getDepartment() != null
                                                && (service.getDepartment().getDeptType() == Department.DeptType.CLINICAL
                                                                || service.getDepartment().getDeptType() == Department.DeptType.PARACLINICAL))
                                .filter(service -> onDutyRoomAssignmentResolver
                                                .findAssignmentsForServiceAt(
                                                                service,
                                                                appointment.getAppointmentDate(),
                                                                appointment.getAppointmentTime())
                                                .isEmpty())
                                .map(Service::getServiceName)
                                .filter(Objects::nonNull)
                                .toList();

                if (!missingServices.isEmpty()) {
                        throw new RuntimeException("Không thể thanh toán " + label
                                        + ": chưa có bác sĩ/phòng trực cho dịch vụ "
                                        + String.join(", ", missingServices));
                }
        }

        private Room resolveOfficialRoomForService(Service service, Appointment appointment) {
                if (service == null) {
                        return null;
                }

                if (appointment != null) {
                        Room preferredRoom = resolvePreferredRoomForService(service, appointment);
                        if (preferredRoom != null) {
                                return preferredRoom;
                        }
                }

                return findOptimalRoomForService(service, appointment);
        }

        private Room resolvePreferredRoomForService(Service service, Appointment appointment) {
                if (service == null || appointment == null) {
                        return null;
                }

                if (service.getDepartment() != null
                                && service.getDepartment().getDeptType() == Department.DeptType.CLINICAL) {
                        if (findOnDutyAssignmentForRoom(service, appointment.getRoom(), appointment).isPresent()) {
                                return appointment.getRoom();
                        }

                        return findOptimalRoomForService(service, appointment);
                }

                if (findOnDutyAssignmentForRoom(service, appointment.getParaclinicalRoom(), appointment).isPresent()) {
                        return appointment.getParaclinicalRoom();
                }

                if (extractServicesByDeptType(appointment.getServices(), Department.DeptType.CLINICAL).isEmpty()) {
                        if (findOnDutyAssignmentForRoom(service, appointment.getRoom(), appointment).isPresent()) {
                                return appointment.getRoom();
                        }
                }

                return findOptimalRoomForService(service, appointment);
        }

        private Optional<OnDutyRoomAssignmentResolver.RoomDoctorAssignment> findOnDutyAssignmentForRoom(
                        Service service,
                        Room room,
                        Appointment appointment) {
                if (service == null || room == null || room.getRoomId() == null) {
                        return Optional.empty();
                }

                if (appointment != null) {
                        return onDutyRoomAssignmentResolver.findAssignmentForRoomAt(
                                        service,
                                        room,
                                        appointment.getAppointmentDate(),
                                        appointment.getAppointmentTime());
                }

                return onDutyRoomAssignmentResolver.findCurrentAssignmentForRoom(service, room, null, null);
        }

        private int roomTypePriority(Room room) {
                if (room == null || room.getRoomType() == null) {
                        return 99;
                }

                return switch (room.getRoomType()) {
                        case "SPECIALTY" -> 0;
                        case "LAB" -> 1;
                        case "GENERAL" -> 2;
                        default -> 3;
                };
        }

        public AppointmentAssignment resolveAppointmentAssignment(Appointment appointment) {
                return resolveAppointmentAssignment(appointment, false);
        }

        private AppointmentAssignment resolveAppointmentAssignment(Appointment appointment, boolean rebalanceTentativeAssignment) {
                if (appointment == null) {
                        return new AppointmentAssignment(null, null, null, null);
                }

                List<Service> clinicalServices = extractServicesByDeptType(
                                appointment.getServices(),
                                Department.DeptType.CLINICAL);
                List<Service> paraclinicalServices = extractServicesByDeptType(
                                appointment.getServices(),
                                Department.DeptType.PARACLINICAL);

                boolean useExistingAssignment = !rebalanceTentativeAssignment || !isTentativeAssignment(appointment);

                Doctor primaryDoctor = useExistingAssignment ? appointment.getDoctor() : null;
                Room primaryRoom = useExistingAssignment ? appointment.getRoom() : null;

                if (!clinicalServices.isEmpty()) {
                        DoctorRoomAssignment clinicalAssignment = resolveDoctorRoomAssignment(
                                        clinicalServices,
                                        primaryDoctor,
                                        primaryRoom,
                                        appointment.getAppointmentDate(),
                                        appointment.getAppointmentTime());
                        primaryDoctor = clinicalAssignment.doctor();
                        primaryRoom = clinicalAssignment.room();
                } else if (!paraclinicalServices.isEmpty()) {
                        DoctorRoomAssignment primaryParaclinicalAssignment = resolveDoctorRoomAssignment(
                                        paraclinicalServices,
                                        primaryDoctor,
                                        primaryRoom,
                                        appointment.getAppointmentDate(),
                                        appointment.getAppointmentTime());
                        primaryDoctor = primaryParaclinicalAssignment.doctor();
                        primaryRoom = primaryParaclinicalAssignment.room();
                }

                Doctor paraclinicalDoctor = useExistingAssignment ? appointment.getParaclinicalDoctor() : null;
                Room paraclinicalRoom = useExistingAssignment ? appointment.getParaclinicalRoom() : null;

                if (!paraclinicalServices.isEmpty()) {
                        Doctor seedParaclinicalDoctor = paraclinicalDoctor;
                        Room seedParaclinicalRoom = paraclinicalRoom;

                        if (seedParaclinicalDoctor == null
                                        && clinicalServices.isEmpty()
                                        && primaryDoctor != null
                                        && isParaclinicalDoctor(primaryDoctor)) {
                                seedParaclinicalDoctor = primaryDoctor;
                        }

                        if (seedParaclinicalRoom == null && clinicalServices.isEmpty()) {
                                seedParaclinicalRoom = primaryRoom;
                        }

                        DoctorRoomAssignment paraclinicalAssignment = resolveDoctorRoomAssignment(
                                        paraclinicalServices,
                                        seedParaclinicalDoctor,
                                        seedParaclinicalRoom,
                                        appointment.getAppointmentDate(),
                                        appointment.getAppointmentTime());
                        paraclinicalDoctor = paraclinicalAssignment.doctor();
                        paraclinicalRoom = paraclinicalAssignment.room();

                        if (clinicalServices.isEmpty()) {
                                primaryDoctor = paraclinicalDoctor;
                                primaryRoom = paraclinicalRoom;
                        }
                } else {
                        paraclinicalDoctor = null;
                        paraclinicalRoom = null;
                }

                return new AppointmentAssignment(primaryDoctor, primaryRoom, paraclinicalDoctor, paraclinicalRoom);
        }

        private boolean isTentativeAssignment(Appointment appointment) {
                if (appointment == null || appointment.getStatus() == null) {
                        return true;
                }

                String status = appointment.getStatus().trim().toUpperCase();
                return "REQUESTED".equals(status) || "CONFIRMED".equals(status) || "CHECKED_IN".equals(status);
        }

        public void reassignAppointmentParticipants(Appointment appointment) {
                if (appointment == null) {
                        return;
                }

                applyAppointmentAssignment(appointment, resolveAppointmentAssignment(appointment));
        }

        /**
         * Load Balancing: tự động gán bác sĩ/phòng tối ưu cho bệnh nhân.
         * Lịch có CLINICAL sẽ giữ doctor/room chính cho lâm sàng và doctor/room riêng cho cận lâm sàng.
         * Lịch chỉ có PARACLINICAL sẽ mirror phân công cận lâm sàng sang doctor/room chính.
         */
        private void autoAssignDoctor(Appointment appointment, boolean rebalanceTentativeAssignment) {
                if (appointment == null) {
                        return;
                }

                applyAppointmentAssignment(appointment, resolveAppointmentAssignment(appointment, rebalanceTentativeAssignment));
        }

        private void applyAppointmentAssignment(Appointment appointment, AppointmentAssignment assignment) {
                appointment.setDoctor(assignment.doctor());
                appointment.setRoom(assignment.room());
                appointment.setParaclinicalDoctor(assignment.paraclinicalDoctor());
                appointment.setParaclinicalRoom(assignment.paraclinicalRoom());
        }

        private DoctorRoomAssignment resolveDoctorRoomAssignment(
                        List<Service> schedulingServices,
                        Doctor currentDoctor,
                        Room currentRoom,
                        LocalDate appointmentDate,
                        LocalTime appointmentTime) {
                if (schedulingServices == null || schedulingServices.isEmpty()) {
                        return new DoctorRoomAssignment(currentDoctor, currentRoom);
                }

                boolean requireSingleRoom = schedulingServices.stream()
                                .anyMatch(service -> service.getDepartment() != null
                                                && service.getDepartment().getDeptType() == Department.DeptType.CLINICAL);

                // Với dịch vụ cận lâm sàng: Appointment chỉ lưu được 1 paraclinicalRoom,
                // nên ta chỉ cần tìm phòng phù hợp cho dịch vụ ĐẦU TIÊN.
                // Các dịch vụ còn lại sẽ được gán phòng riêng ở tầng hiển thị (pickBestRoom).
                List<Service> matchingServices = requireSingleRoom
                        ? schedulingServices
                        : List.of(schedulingServices.get(0));

                java.util.List<OnDutyRoomAssignmentResolver.RoomDoctorAssignment> assignments =
                                onDutyRoomAssignmentResolver.findAssignmentsForServicesAt(
                                                matchingServices,
                                                appointmentDate,
                                                appointmentTime,
                                                requireSingleRoom);

                if (assignments.isEmpty()) {
                        return new DoctorRoomAssignment(null, null);
                }

                if (currentRoom != null && currentRoom.getRoomId() != null) {
                        Optional<OnDutyRoomAssignmentResolver.RoomDoctorAssignment> currentRoomAssignment =
                                        assignments.stream()
                                                        .filter(assignment -> assignment.room() != null
                                                                        && currentRoom.getRoomId().equals(assignment.room().getRoomId()))
                                                        .findFirst();
                        if (currentRoomAssignment.isPresent()) {
                                OnDutyRoomAssignmentResolver.RoomDoctorAssignment assignment = currentRoomAssignment.get();
                                return new DoctorRoomAssignment(assignment.doctor(), assignment.room());
                        }
                }

                if (currentDoctor != null && currentDoctor.getDoctorId() != null) {
                        Optional<OnDutyRoomAssignmentResolver.RoomDoctorAssignment> currentDoctorAssignment =
                                        assignments.stream()
                                                        .filter(assignment -> assignment.doctor() != null
                                                                        && currentDoctor.getDoctorId().equals(assignment.doctor().getDoctorId()))
                                                        .findFirst();
                        if (currentDoctorAssignment.isPresent()) {
                                OnDutyRoomAssignmentResolver.RoomDoctorAssignment assignment = currentDoctorAssignment.get();
                                return new DoctorRoomAssignment(assignment.doctor(), assignment.room());
                        }
                }

                OnDutyRoomAssignmentResolver.RoomDoctorAssignment bestAssignment = assignments.stream()
                                .min(java.util.Comparator
                                                .comparingInt((OnDutyRoomAssignmentResolver.RoomDoctorAssignment assignment) ->
                                                                roomLoadScore(assignment.room()))
                                                .thenComparingInt(assignment -> roomTypePriority(assignment.room()))
                                                .thenComparing(assignment -> assignment.room() != null
                                                                ? assignment.room().getRoomId()
                                                                : null,
                                                                java.util.Comparator.nullsLast(String::compareTo)))
                                .orElse(null);

                return bestAssignment == null
                                ? new DoctorRoomAssignment(null, null)
                                : new DoctorRoomAssignment(bestAssignment.doctor(), bestAssignment.room());
        }

        private List<Service> extractSchedulingServices(List<Service> services) {
                List<Service> clinicalServices = extractServicesByDeptType(services, Department.DeptType.CLINICAL);
                if (!clinicalServices.isEmpty()) {
                        return clinicalServices;
                }
                return extractServicesByDeptType(services, Department.DeptType.PARACLINICAL);
        }

        private List<Service> extractServicesByDeptType(List<Service> services, Department.DeptType deptType) {
                if (services == null || services.isEmpty()) {
                        return List.of();
                }

                return services.stream()
                                .filter(service -> service.getDepartment() != null
                                                && service.getDepartment().getDeptType() == deptType)
                                .toList();
        }

        private boolean isParaclinicalDoctor(Doctor doctor) {
                if (doctor == null) {
                        return false;
                }

                return DoctorDepartmentClassifier.isParaclinical(doctor);
        }

        private Room getAssignedRoom(Appointment appointment) {
                if (appointment == null) {
                        return null;
                }
                if (appointment.getRoom() != null) {
                        return appointment.getRoom();
                }

                List<Service> schedulingServices = extractSchedulingServices(appointment.getServices());
                if (schedulingServices.isEmpty()) {
                        return null;
                }

                return onDutyRoomAssignmentResolver
                                .findAssignmentsForServicesAt(
                                                schedulingServices,
                                                appointment.getAppointmentDate(),
                                                appointment.getAppointmentTime(),
                                                true)
                                .stream()
                                .map(OnDutyRoomAssignmentResolver.RoomDoctorAssignment::room)
                                .filter(Objects::nonNull)
                                .findFirst()
                                .orElse(null);
        }

        private void assignQueueNumberIfMissing(Appointment appointment) {
                if (appointment == null || appointment.getQueueNumber() != null) {
                        return;
                }

                Room assignedRoom = getAssignedRoom(appointment);
                if (assignedRoom == null || assignedRoom.getRoomId() == null) {
                        return;
                }

                LocalDate queueDate = appointment.getAppointmentDate() != null
                                ? appointment.getAppointmentDate()
                                : ClinicTime.today();
                Integer maxQueueNumber = appointmentRepository.findMaxQueueNumberByRoomIdAndDate(
                                assignedRoom.getRoomId(),
                                queueDate);
                appointment.setQueueNumber((maxQueueNumber != null ? maxQueueNumber : 0) + 1);
        }

        private void incrementQueueLengthForOfficialRooms(
                        Appointment appointment,
                        java.util.List<g2.be.entity.ServiceOrder> serviceOrders) {
                java.util.Set<String> incrementedRoomIds = new java.util.HashSet<>();
                incrementRoomQueueLength(getAssignedRoom(appointment), incrementedRoomIds);

                if (serviceOrders == null) {
                        return;
                }

                for (g2.be.entity.ServiceOrder order : serviceOrders) {
                        if (order != null) {
                                incrementRoomQueueLength(order.getAssignedRoom(), incrementedRoomIds);
                        }
                }
        }

        private void incrementRoomQueueLength(Room room, java.util.Set<String> incrementedRoomIds) {
                if (room == null || room.getRoomId() == null || !incrementedRoomIds.add(room.getRoomId())) {
                        return;
                }

                room.setCurrentQueueLength((room.getCurrentQueueLength() != null ? room.getCurrentQueueLength() : 0) + 1);
                roomRepository.save(room);
        }

        public record AppointmentAssignment(Doctor doctor,
                                            Room room,
                                            Doctor paraclinicalDoctor,
                                            Room paraclinicalRoom) {
        }

        private record DoctorRoomAssignment(Doctor doctor, Room room) {
        }
}
