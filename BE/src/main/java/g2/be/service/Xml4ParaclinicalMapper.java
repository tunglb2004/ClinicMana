package g2.be.service;

import g2.be.dto.request.Xml4ParaclinicalRequest;
import g2.be.entity.ServiceOrder;
import g2.be.util.ServiceOrderResultFiles;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper cho Bảng 4 (XML4) - Chi tiết dịch vụ cận lâm sàng
 * 
 * Chuyển đổi ServiceOrder (DB) → Xml4ParaclinicalRequest (DTO)
 * 
 * Nguyên tắc: Data Honesty
 * - Chưa có kết quả → Các trường GIA_TRI, NGAY_KQ = null
 * - Không bịa giá trị giả
 */
@Service
@RequiredArgsConstructor
public class Xml4ParaclinicalMapper {

    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    /**
     * Chuyển đổi danh sách ServiceOrder → List<Xml4ParaclinicalRequest>
     * 
     * @param serviceOrders Danh sách dịch vụ đã thực hiện
     * @param maLk Mã đợt điều trị (từ XML1)
     * @return Danh sách DTO XML4
     */
    public List<Xml4ParaclinicalRequest> mapToXml4List(List<ServiceOrder> serviceOrders, String maLk) {
        return serviceOrders.stream()
                .map(order -> mapToXml4(order, maLk))
                .collect(Collectors.toList());
    }

    /**
     * Chuyển đổi một ServiceOrder → Xml4ParaclinicalRequest
     * 
     * @param order Dịch vụ đã thực hiện
     * @param maLk Mã đợt điều trị
     * @return DTO XML4
     */
    public Xml4ParaclinicalRequest mapToXml4(ServiceOrder order, String maLk) {
        // Stt sẽ được gán lại khi tạo batch gửi đi (1, 2, 3...)
        return Xml4ParaclinicalRequest.builder()
                .maLk(maLk)
                .stt(null) // Sẽ được gán khi tạo batch
                .maDichVu(order.getService().getServiceId())
                
                // Test details - dùng serviceId làm mã chỉ số, serviceName làm tên chỉ số
                .maChiSo(order.getService().getServiceId())
                .tenChiSo(order.getService().getServiceName())
                
                // Kết quả - có thể null nếu chưa hoàn thành
                .giaTri(order.getResultNote()) // Có thể null nếu status = PENDING
                .donViDo("N/A") // ServiceOrder không có đơn vị đo
                .moTa(joinResultFileUrls(order)) // URL file kết quả (nếu có)
                .ketLuan(null) // ServiceOrder không có field kết luận
                .ngayKq(formatDateTimeIfCompleted(order)) // Null nếu chưa hoàn thành
                .maBsDocKq(order.getTechnicianId())
                .duPhong(null)
                .build();
    }

    /**
     * Format datetime sang yyyyMMddHHmm
     * Chỉ trả về giá trị nếu ServiceOrder đã COMPLETED
     */
    private String formatDateTimeIfCompleted(ServiceOrder order) {
        if ("COMPLETED".equals(order.getStatus()) && order.getResultNote() != null) {
            return order.getCreatedAt().format(DATETIME_FORMAT);
        }
        return null;
    }

    /**
     * Gán lại STT cho danh sách XML4
     * (Dùng khi tạo batch trước khi gửi)
     */
    public void assignStt(List<Xml4ParaclinicalRequest> xml4List) {
        for (int i = 0; i < xml4List.size(); i++) {
            xml4List.get(i).setStt(i + 1);
        }
    }

    private String joinResultFileUrls(ServiceOrder order) {
        List<String> fileUrls = ServiceOrderResultFiles.parse(order.getResultFileUrl());
        if (fileUrls.isEmpty()) {
            return null;
        }
        return String.join("\n", fileUrls);
    }
}
