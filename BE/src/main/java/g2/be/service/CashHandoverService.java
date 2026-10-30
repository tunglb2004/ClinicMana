package g2.be.service;

import g2.be.dto.request.CashHandoverRequest;
import g2.be.dto.response.CashHandoverResponse;
import g2.be.dto.response.CashierDaySummaryResponse;
import g2.be.dto.response.FinancialResponse;
import g2.be.dto.response.ManagerCashierSummaryResponse;
import g2.be.dto.response.PaymentMethodBreakdownResponse;
import g2.be.entity.CashHandover;
import g2.be.entity.Invoice;
import g2.be.entity.Role;
import g2.be.entity.User;
import g2.be.exception.AppException;
import g2.be.exception.ErrorCode;
import g2.be.repository.CashHandoverRepository;
import g2.be.repository.InvoiceRepository;
import g2.be.repository.UserRepository;
import g2.be.util.ClinicTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CashHandoverService {

    private static final Set<String> TRANSFER_METHODS = Set.of("QR", "QR_SEPAY", "TRANSFER", "BANK_TRANSFER");
    private static final int RECENT_INVOICE_LIMIT = 25;

    private final CashHandoverRepository cashHandoverRepository;
    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public CashierDaySummaryResponse getCashierDaySummary(LocalDate businessDate,
                                                          String cashierId,
                                                          String cashierStaffId) {
        LocalDate targetDate = businessDate != null ? businessDate : ClinicTime.today();
        return getCashierSummary(new BusinessDateRange(targetDate, targetDate), cashierId, cashierStaffId);
    }

    private CashierDaySummaryResponse getCashierSummary(BusinessDateRange range,
                                                        String cashierId,
                                                        String cashierStaffId) {
        LocalDateTime from = range.fromDate().atStartOfDay();
        LocalDateTime to = range.toDate().plusDays(1).atStartOfDay();
        List<Invoice> invoices = invoiceRepository.findPaidInvoicesForCashierBetween(cashierId, cashierStaffId, from, to);

        BigDecimal totalCollected = BigDecimal.ZERO;
        BigDecimal cashCollected = BigDecimal.ZERO;
        BigDecimal transferCollected = BigDecimal.ZERO;
        BigDecimal otherCollected = BigDecimal.ZERO;
        long cashInvoiceCount = 0;
        long transferInvoiceCount = 0;
        long otherInvoiceCount = 0;
        Map<String, MethodAccumulator> methodBreakdown = new HashMap<>();

        for (Invoice invoice : invoices) {
            BigDecimal amount = payableAmount(invoice);
            String method = normalizeMethod(invoice.getPaymentMethod());
            totalCollected = totalCollected.add(amount);
            methodBreakdown.computeIfAbsent(method, ignored -> new MethodAccumulator())
                    .add(amount);

            if (isCashMethod(method)) {
                cashCollected = cashCollected.add(amount);
                cashInvoiceCount++;
            } else if (isTransferMethod(method)) {
                transferCollected = transferCollected.add(amount);
                transferInvoiceCount++;
            } else {
                otherCollected = otherCollected.add(amount);
                otherInvoiceCount++;
            }
        }

        BigDecimal pendingHandovers = sumHandovers(range, cashierId, Set.of("PENDING"));
        BigDecimal confirmedHandovers = sumHandovers(range, cashierId, Set.of("CONFIRMED"));
        BigDecimal cashRemaining = cashCollected.subtract(pendingHandovers).subtract(confirmedHandovers);
        if (cashRemaining.signum() < 0) {
            cashRemaining = BigDecimal.ZERO;
        }

        List<PaymentMethodBreakdownResponse> breakdownResponses = methodBreakdown.entrySet().stream()
                .map(entry -> PaymentMethodBreakdownResponse.builder()
                        .paymentMethod(entry.getKey())
                        .amount(entry.getValue().amount())
                        .invoiceCount(entry.getValue().invoiceCount())
                        .build())
                .sorted(Comparator.comparing(PaymentMethodBreakdownResponse::getPaymentMethod))
                .toList();

        List<FinancialResponse> recentInvoices = invoices.stream()
                .limit(RECENT_INVOICE_LIMIT)
                .map(FinancialResponse::fromEntity)
                .toList();

        return CashierDaySummaryResponse.builder()
                .businessDate(range.fromDate())
                .totalCollected(totalCollected)
                .cashCollected(cashCollected)
                .transferCollected(transferCollected)
                .otherCollected(otherCollected)
                .pendingHandovers(pendingHandovers)
                .confirmedHandovers(confirmedHandovers)
                .cashRemaining(cashRemaining)
                .invoiceCount(invoices.size())
                .cashInvoiceCount(cashInvoiceCount)
                .transferInvoiceCount(transferInvoiceCount)
                .otherInvoiceCount(otherInvoiceCount)
                .methodBreakdown(breakdownResponses)
                .recentInvoices(recentInvoices)
                .build();
    }

    @Transactional
    public CashHandoverResponse createCashHandover(CashHandoverRequest request, Long currentUserId) {
        if (request == null || request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.CASH_HANDOVER_INVALID_AMOUNT);
        }
        if (currentUserId == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        if (Objects.equals(currentUserId, request.getToUserId())) {
            throw new AppException(ErrorCode.CASH_HANDOVER_SELF_TRANSFER);
        }

        LocalDate businessDate = request.getBusinessDate() != null ? request.getBusinessDate() : ClinicTime.today();
        User toCashier = findActiveCashier(request.getToUserId());
        User fromCashier = findActiveCashier(currentUserId);

        String cashierId = String.valueOf(currentUserId);
        CashierDaySummaryResponse summary = getCashierDaySummary(
                businessDate,
                cashierId,
                staffIdOf(fromCashier)
        );
        if (request.getAmount().compareTo(summary.getCashRemaining()) > 0) {
            throw new AppException(ErrorCode.CASH_HANDOVER_AMOUNT_EXCEEDS_REMAINING);
        }

        CashHandover handover = CashHandover.builder()
                .businessDate(businessDate)
                .fromCashierUserId(currentUserId)
                .toCashierUserId(toCashier.getUserId())
                .amount(request.getAmount())
                .status("PENDING")
                .note(trimToNull(request.getNote()))
                .build();

        return toResponse(cashHandoverRepository.save(handover), currentUserId);
    }

    @Transactional(readOnly = true)
    public List<CashHandoverResponse> getCashHandovers(LocalDate businessDate, String scope, Long currentUserId) {
        if (currentUserId == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        LocalDate targetDate = businessDate != null ? businessDate : ClinicTime.today();
        String normalizedScope = scope == null || scope.isBlank() ? "mine" : scope.trim().toLowerCase();

        List<CashHandover> handovers = "incoming".equals(normalizedScope)
                ? cashHandoverRepository.findByBusinessDateAndToCashierUserIdOrderByCreatedAtDesc(targetDate, currentUserId)
                : cashHandoverRepository.findByBusinessDateAndFromCashierUserIdOrderByCreatedAtDesc(targetDate, currentUserId);

        return toResponses(handovers, currentUserId);
    }

    @Transactional
    public CashHandoverResponse confirmCashHandover(Long handoverId, Long currentUserId) {
        CashHandover handover = getPendingHandover(handoverId);
        if (!Objects.equals(handover.getToCashierUserId(), currentUserId)) {
            throw new AppException(ErrorCode.CASH_HANDOVER_ACCESS_DENIED);
        }

        handover.setStatus("CONFIRMED");
        handover.setConfirmedAt(ClinicTime.now());
        return toResponse(cashHandoverRepository.save(handover), currentUserId);
    }

    @Transactional
    public CashHandoverResponse cancelCashHandover(Long handoverId, Long currentUserId) {
        CashHandover handover = getPendingHandover(handoverId);
        if (!Objects.equals(handover.getFromCashierUserId(), currentUserId)) {
            throw new AppException(ErrorCode.CASH_HANDOVER_ACCESS_DENIED);
        }

        handover.setStatus("CANCELLED");
        handover.setCancelledAt(ClinicTime.now());
        return toResponse(cashHandoverRepository.save(handover), currentUserId);
    }

    @Transactional(readOnly = true)
    public ManagerCashierSummaryResponse getManagerCashierDaySummary(LocalDate businessDate, Long cashierUserId) {
        return getManagerCashierSummary(businessDate, null, null, cashierUserId);
    }

    @Transactional(readOnly = true)
    public ManagerCashierSummaryResponse getManagerCashierSummary(LocalDate businessDate,
                                                                  LocalDate fromDate,
                                                                  LocalDate toDate,
                                                                  Long cashierUserId) {
        User cashier = findActiveCashier(cashierUserId);
        CashierDaySummaryResponse summary = getCashierSummary(
                resolveDateRange(businessDate, fromDate, toDate),
                String.valueOf(cashier.getUserId()),
                staffIdOf(cashier)
        );
        return toManagerCashierSummary(cashier, summary);
    }

    @Transactional(readOnly = true)
    public List<ManagerCashierSummaryResponse> getManagerCashierSummaries(LocalDate businessDate) {
        return getManagerCashierSummaries(businessDate, null, null);
    }

    @Transactional(readOnly = true)
    public List<ManagerCashierSummaryResponse> getManagerCashierSummaries(LocalDate businessDate,
                                                                          LocalDate fromDate,
                                                                          LocalDate toDate) {
        BusinessDateRange range = resolveDateRange(businessDate, fromDate, toDate);
        return userRepository.findByRoleAndIsActive("ROLE_CASHIER", true, false, Pageable.unpaged())
                .getContent()
                .stream()
                .map(cashier -> toManagerCashierSummary(
                        cashier,
                        getCashierSummary(range, String.valueOf(cashier.getUserId()), staffIdOf(cashier))
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CashHandoverResponse> getManagerCashHandovers(LocalDate businessDate, Long cashierUserId, String status) {
        return getManagerCashHandovers(businessDate, null, null, cashierUserId, status);
    }

    @Transactional(readOnly = true)
    public List<CashHandoverResponse> getManagerCashHandovers(LocalDate businessDate,
                                                              LocalDate fromDate,
                                                              LocalDate toDate,
                                                              Long cashierUserId,
                                                              String status) {
        BusinessDateRange range = resolveDateRange(businessDate, fromDate, toDate);
        List<CashHandover> handovers = cashHandoverRepository.findManagerHandovers(
                range.fromDate(),
                range.toDate(),
                cashierUserId,
                normalizeStatusFilter(status)
        );
        return toResponses(handovers, null);
    }

    private CashHandover getPendingHandover(Long handoverId) {
        CashHandover handover = cashHandoverRepository.findById(handoverId)
                .orElseThrow(() -> new AppException(ErrorCode.CASH_HANDOVER_NOT_FOUND));
        if (!"PENDING".equals(handover.getStatus())) {
            throw new AppException(ErrorCode.CASH_HANDOVER_INVALID_STATUS);
        }
        return handover;
    }

    private BigDecimal sumHandovers(BusinessDateRange range, String cashierId, Set<String> statuses) {
        if (cashierId == null || cashierId.isBlank()) {
            return safeAmount(cashHandoverRepository.sumAmountByBusinessDateBetweenAndStatusIn(
                    range.fromDate(),
                    range.toDate(),
                    statuses
            ));
        }

        return safeAmount(cashHandoverRepository.sumAmountByBusinessDateBetweenAndFromCashierUserIdAndStatusIn(
                range.fromDate(),
                range.toDate(),
                Long.valueOf(cashierId),
                statuses
        ));
    }

    private BusinessDateRange resolveDateRange(LocalDate businessDate, LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null && toDate == null) {
            LocalDate targetDate = businessDate != null ? businessDate : ClinicTime.today();
            return new BusinessDateRange(targetDate, targetDate);
        }

        LocalDate from = fromDate != null ? fromDate : toDate;
        LocalDate to = toDate != null ? toDate : from;
        if (from.isAfter(to)) {
            return new BusinessDateRange(to, from);
        }
        return new BusinessDateRange(from, to);
    }

    private CashHandoverResponse toResponse(CashHandover handover, Long currentUserId) {
        Map<Long, String> names = getUserNames(List.of(handover));
        return CashHandoverResponse.fromEntity(
                handover,
                names.get(handover.getFromCashierUserId()),
                names.get(handover.getToCashierUserId()),
                currentUserId
        );
    }

    private List<CashHandoverResponse> toResponses(List<CashHandover> handovers, Long currentUserId) {
        Map<Long, String> names = getUserNames(handovers);
        return handovers.stream()
                .map(handover -> CashHandoverResponse.fromEntity(
                        handover,
                        names.get(handover.getFromCashierUserId()),
                        names.get(handover.getToCashierUserId()),
                        currentUserId
                ))
                .toList();
    }

    private Map<Long, String> getUserNames(List<CashHandover> handovers) {
        List<Long> userIds = handovers.stream()
                .flatMap(handover -> List.of(handover.getFromCashierUserId(), handover.getToCashierUserId()).stream())
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (userIds.isEmpty()) {
            return Map.of();
        }

        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getUserId, User::getFullName));
    }

    private ManagerCashierSummaryResponse toManagerCashierSummary(User cashier, CashierDaySummaryResponse summary) {
        return ManagerCashierSummaryResponse.builder()
                .cashierUserId(cashier.getUserId())
                .cashierName(cashier.getFullName())
                .cashierStaffId(cashier.getStaffId())
                .summary(summary)
                .build();
    }

    private String normalizeStatusFilter(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return status.trim().toUpperCase();
    }

    private User findActiveCashier(Long userId) {
        if (userId == null) {
            throw new AppException(ErrorCode.CASHIER_NOT_FOUND);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.CASHIER_NOT_FOUND));
        if (Boolean.TRUE.equals(user.getIsDeleted()) || Boolean.FALSE.equals(user.getIsActive()) || !hasCashierRole(user)) {
            throw new AppException(ErrorCode.CASHIER_NOT_FOUND);
        }
        return user;
    }

    private boolean hasCashierRole(User user) {
        return user.getRoles() != null && user.getRoles().stream()
                .map(Role::getRoleName)
                .anyMatch("ROLE_CASHIER"::equals);
    }

    private String staffIdOf(User user) {
        return user == null || user.getStaffId() == null || user.getStaffId().isBlank()
                ? null
                : user.getStaffId();
    }

    private String normalizeMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.isBlank()) {
            return "UNKNOWN";
        }
        return paymentMethod.trim().toUpperCase();
    }

    private boolean isCashMethod(String method) {
        return "CASH".equals(method);
    }

    private boolean isTransferMethod(String method) {
        return TRANSFER_METHODS.contains(method);
    }

    private BigDecimal payableAmount(Invoice invoice) {
        if (invoice == null) {
            return BigDecimal.ZERO;
        }
        if (invoice.getCustomerPaid() != null) {
            return invoice.getCustomerPaid();
        }
        return safeAmount(invoice.getTotalServiceFee());
    }

    private BigDecimal safeAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record BusinessDateRange(LocalDate fromDate, LocalDate toDate) {
    }

    private static class MethodAccumulator {
        private BigDecimal amount = BigDecimal.ZERO;
        private long invoiceCount;

        void add(BigDecimal value) {
            amount = amount.add(value == null ? BigDecimal.ZERO : value);
            invoiceCount++;
        }

        BigDecimal amount() {
            return amount;
        }

        long invoiceCount() {
            return invoiceCount;
        }
    }
}
