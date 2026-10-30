package g2.be.service;

import g2.be.dto.request.CashHandoverRequest;
import g2.be.dto.response.CashHandoverResponse;
import g2.be.dto.response.CashierDaySummaryResponse;
import g2.be.dto.response.ManagerCashierSummaryResponse;
import g2.be.entity.CashHandover;
import g2.be.entity.Invoice;
import g2.be.entity.Role;
import g2.be.entity.User;
import g2.be.exception.AppException;
import g2.be.exception.ErrorCode;
import g2.be.repository.CashHandoverRepository;
import g2.be.repository.InvoiceRepository;
import g2.be.repository.UserRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CashHandoverServiceTest {

    @Mock
    private CashHandoverRepository cashHandoverRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CashHandoverService cashHandoverService;

    @Test
    void getCashierDaySummarySeparatesCashTransferAndRemainingCash() {
        LocalDate date = LocalDate.of(2026, 4, 28);
        when(invoiceRepository.findPaidInvoicesForCashierBetween(eq("1"), eq("NV-1"), any(), any()))
                .thenReturn(List.of(
                        invoice("INV-CASH", "CASH", "100000"),
                        invoice("INV-QR", "QR_SEPAY", "200000"),
                        invoice("INV-OTHER", "CARD", "50000")
                ));
        when(cashHandoverRepository.sumAmountByBusinessDateBetweenAndFromCashierUserIdAndStatusIn(eq(date), eq(date), eq(1L), eq(Set.of("PENDING"))))
                .thenReturn(new BigDecimal("30000"));
        when(cashHandoverRepository.sumAmountByBusinessDateBetweenAndFromCashierUserIdAndStatusIn(eq(date), eq(date), eq(1L), eq(Set.of("CONFIRMED"))))
                .thenReturn(new BigDecimal("20000"));

        CashierDaySummaryResponse response = cashHandoverService.getCashierDaySummary(date, "1", "NV-1");

        assertEquals(0, new BigDecimal("350000").compareTo(response.getTotalCollected()));
        assertEquals(0, new BigDecimal("100000").compareTo(response.getCashCollected()));
        assertEquals(0, new BigDecimal("200000").compareTo(response.getTransferCollected()));
        assertEquals(0, new BigDecimal("50000").compareTo(response.getOtherCollected()));
        assertEquals(0, new BigDecimal("50000").compareTo(response.getCashRemaining()));
        assertEquals(3, response.getInvoiceCount());
        assertEquals(1, response.getCashInvoiceCount());
        assertEquals(1, response.getTransferInvoiceCount());
        assertEquals(1, response.getOtherInvoiceCount());
    }

    @Test
    void createCashHandoverRejectsAmountAboveRemainingCash() {
        LocalDate date = LocalDate.of(2026, 4, 28);
        User fromCashier = cashier(1L, "NV-1", "Thu ngan 1");
        User toCashier = cashier(2L, "NV-2", "Thu ngan 2");
        when(userRepository.findById(1L)).thenReturn(Optional.of(fromCashier));
        when(userRepository.findById(2L)).thenReturn(Optional.of(toCashier));
        when(invoiceRepository.findPaidInvoicesForCashierBetween(eq("1"), eq("NV-1"), any(), any()))
                .thenReturn(List.of(invoice("INV-CASH", "CASH", "100000")));
        when(cashHandoverRepository.sumAmountByBusinessDateBetweenAndFromCashierUserIdAndStatusIn(eq(date), eq(date), eq(1L), any()))
                .thenReturn(BigDecimal.ZERO);

        CashHandoverRequest request = CashHandoverRequest.builder()
                .businessDate(date)
                .toUserId(2L)
                .amount(new BigDecimal("120000"))
                .build();

        AppException exception = assertThrows(AppException.class,
                () -> cashHandoverService.createCashHandover(request, 1L));

        assertEquals(ErrorCode.CASH_HANDOVER_AMOUNT_EXCEEDS_REMAINING, exception.getErrorCode());
    }

    @Test
    void createCashHandoverSavesPendingHandover() {
        LocalDate date = LocalDate.of(2026, 4, 28);
        User fromCashier = cashier(1L, "NV-1", "Thu ngan 1");
        User toCashier = cashier(2L, "NV-2", "Thu ngan 2");
        when(userRepository.findById(1L)).thenReturn(Optional.of(fromCashier));
        when(userRepository.findById(2L)).thenReturn(Optional.of(toCashier));
        when(userRepository.findAllById(any())).thenReturn(List.of(fromCashier, toCashier));
        when(invoiceRepository.findPaidInvoicesForCashierBetween(eq("1"), eq("NV-1"), any(), any()))
                .thenReturn(List.of(invoice("INV-CASH", "CASH", "100000")));
        when(cashHandoverRepository.sumAmountByBusinessDateBetweenAndFromCashierUserIdAndStatusIn(eq(date), eq(date), eq(1L), any()))
                .thenReturn(BigDecimal.ZERO);
        when(cashHandoverRepository.save(any(CashHandover.class))).thenAnswer(invocation -> {
            CashHandover handover = invocation.getArgument(0);
            handover.setHandoverId(10L);
            handover.setCreatedAt(LocalDateTime.of(2026, 4, 28, 8, 0));
            return handover;
        });

        CashHandoverResponse response = cashHandoverService.createCashHandover(
                CashHandoverRequest.builder()
                        .businessDate(date)
                        .toUserId(2L)
                        .amount(new BigDecimal("80000"))
                        .note("Ca sang")
                        .build(),
                1L
        );

        assertEquals(10L, response.getHandoverId());
        assertEquals("PENDING", response.getStatus());
        assertEquals("Thu ngan 2", response.getToCashierName());
        assertTrue(response.isCanCancel());
        verify(cashHandoverRepository).save(any(CashHandover.class));
    }

    @Test
    void confirmCashHandoverRequiresReceivingCashier() {
        CashHandover handover = CashHandover.builder()
                .handoverId(10L)
                .businessDate(LocalDate.of(2026, 4, 28))
                .fromCashierUserId(1L)
                .toCashierUserId(2L)
                .amount(new BigDecimal("80000"))
                .status("PENDING")
                .build();
        when(cashHandoverRepository.findById(10L)).thenReturn(Optional.of(handover));

        AppException exception = assertThrows(AppException.class,
                () -> cashHandoverService.confirmCashHandover(10L, 1L));

        assertEquals(ErrorCode.CASH_HANDOVER_ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    void getManagerCashierSummariesReturnsAllActiveCashiers() {
        LocalDate date = LocalDate.of(2026, 4, 28);
        User cashier1 = cashier(1L, "NV-1", "Thu ngan 1");
        User cashier2 = cashier(2L, "NV-2", "Thu ngan 2");
        when(userRepository.findByRoleAndIsActive("ROLE_CASHIER", true, false, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(cashier1, cashier2)));
        when(invoiceRepository.findPaidInvoicesForCashierBetween(eq("1"), eq("NV-1"), any(), any()))
                .thenReturn(List.of(invoice("INV-CASH", "CASH", "100000")));
        when(invoiceRepository.findPaidInvoicesForCashierBetween(eq("2"), eq("NV-2"), any(), any()))
                .thenReturn(List.of(invoice("INV-QR", "QR", "200000")));

        List<ManagerCashierSummaryResponse> responses = cashHandoverService.getManagerCashierSummaries(date);

        assertEquals(2, responses.size());
        assertEquals(1L, responses.get(0).getCashierUserId());
        assertEquals("Thu ngan 1", responses.get(0).getCashierName());
        assertEquals(0, new BigDecimal("100000").compareTo(responses.get(0).getSummary().getCashCollected()));
        assertEquals(0, new BigDecimal("200000").compareTo(responses.get(1).getSummary().getTransferCollected()));
    }

    private Invoice invoice(String invoiceId, String paymentMethod, String amount) {
        return Invoice.builder()
                .invoiceId(invoiceId)
                .paymentMethod(paymentMethod)
                .customerPaid(new BigDecimal(amount))
                .build();
    }

    private User cashier(Long userId, String staffId, String fullName) {
        return User.builder()
                .userId(userId)
                .staffId(staffId)
                .fullName(fullName)
                .isActive(true)
                .isDeleted(false)
                .roles(Set.of(Role.builder().roleName("ROLE_CASHIER").build()))
                .build();
    }

}
