package g2.be.controller;

import g2.be.dto.response.ApiResponse;
import g2.be.dto.response.FinancialDetailResponse;
import g2.be.dto.response.FinancialResponse;
import g2.be.dto.response.ManagerCashierSummaryResponse;
import g2.be.dto.response.PageResponse;
import g2.be.dto.response.ReceiptResponse;
import g2.be.repository.UserRepository;
import g2.be.service.CashHandoverService;
import g2.be.service.FinancialService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialControllerTest {

    @Mock
    private FinancialService financialService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CashHandoverService cashHandoverService;

    @InjectMocks
    private FinancialController financialController;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getInvoicesReturnsPagedFinancialRecords() {
        PageResponse<FinancialResponse> pageResponse = PageResponse.<FinancialResponse>builder()
                .content(List.of(mock(FinancialResponse.class)))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();

        when(financialService.getInvoices(0, 10, "INV", "PAID", null, null, false, 0)).thenReturn(pageResponse);

        ApiResponse<PageResponse<FinancialResponse>> response = financialController.getInvoices(1, 10, "INV", "PAID");

        assertTrue(response.isSuccess());
        assertSame(pageResponse, response.getData());
        verify(financialService).getInvoices(0, 10, "INV", "PAID", null, null, false, 0);
    }

    @Test
    void getInvoiceDetailReturnsFinancialDetail() {
        FinancialDetailResponse detailResponse = mock(FinancialDetailResponse.class);

        when(financialService.getInvoiceDetail("INV-001")).thenReturn(detailResponse);

        ApiResponse<FinancialDetailResponse> response = financialController.getInvoiceDetail("INV-001");

        assertTrue(response.isSuccess());
        assertSame(detailResponse, response.getData());
    }

    @Test
    void issueReceiptReturnsReceiptResponse() {
        ReceiptResponse receiptResponse = mock(ReceiptResponse.class);

        when(financialService.issueReceipt("INV-001")).thenReturn(receiptResponse);

        ApiResponse<ReceiptResponse> response = financialController.issueReceipt("INV-001");

        assertTrue(response.isSuccess());
        assertEquals("Xuất phiếu thu thành công.", response.getMessage());
        assertSame(receiptResponse, response.getData());
    }

    @Test
    void getManagerCashierSummariesReturnsServiceData() {
        LocalDate date = LocalDate.of(2026, 4, 28);
        List<ManagerCashierSummaryResponse> summaries = List.of(mock(ManagerCashierSummaryResponse.class));
        when(cashHandoverService.getManagerCashierSummaries(date, null, null)).thenReturn(summaries);

        ApiResponse<List<ManagerCashierSummaryResponse>> response =
                financialController.getManagerCashierSummaries(date, null, null);

        assertTrue(response.isSuccess());
        assertSame(summaries, response.getData());
        verify(cashHandoverService).getManagerCashierSummaries(date, null, null);
    }

}
