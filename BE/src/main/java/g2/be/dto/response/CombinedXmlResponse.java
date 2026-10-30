package g2.be.dto.response;

import g2.be.dto.request.Xml1MedicalRecordRequest;
import g2.be.dto.request.Xml4ParaclinicalRequest;
import lombok.*;

import java.util.List;

/**
 * Response tổng hợp XML1 + XML4
 * 
 * Đây là gói dữ liệu hoàn chỉnh gửi sang API BHXH:
 * - XML1: Thông tin tổng hợp đợt khám (1 record)
 * - XML4: Danh sách kết quả xét nghiệm (nhiều records)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CombinedXmlResponse {

    /**
     * Bảng 1 - Chỉ tiêu tổng hợp KBCB
     */
    private Xml1MedicalRecordRequest xml1;

    /**
     * Bảng 4 - Chi tiết dịch vụ cận lâm sàng
     * Danh sách các kết quả xét nghiệm, chẩn đoán hình ảnh
     */
    private List<Xml4ParaclinicalRequest> xml4;

    /**
     * Metadata
     */
    private MetaData metadata;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MetaData {
        private String maLk;
        private String patientName;
        private Integer totalParaclinicalTests;
        private Integer completedTests;
        private Integer pendingTests;
        private String message;
    }
}
