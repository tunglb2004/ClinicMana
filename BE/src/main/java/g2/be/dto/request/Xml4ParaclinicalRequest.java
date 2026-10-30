package g2.be.dto.request;

import lombok.*;

/**
 * DTO cho Bảng 4 - Chi tiết dịch vụ cận lâm sàng (XML4)
 * 
 * Bảng này chứa kết quả xét nghiệm, chẩn đoán hình ảnh, thăm dò chức năng
 * Là minh chứng y khoa để BHXH xác nhận chẩn đoán
 * 
 * Nguyên tắc: Data Honesty
 * - Chưa có kết quả → GIA_TRI và NGAY_KQ = null
 * - Có thể bổ sung sau trong vòng 30 ngày
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Xml4ParaclinicalRequest {

    /**
     * 1. MA_LK - Mã đợt điều trị (Link với XML1)
     */
    private String maLk;

    /**
     * 2. STT - Số thứ tự
     */
    private Integer stt;

    /**
     * 3. MA_DICH_VU - Mã dịch vụ kỹ thuật cận lâm sàng
     * Theo Phụ lục 1 QĐ 7603/QĐ-BYT
     * VD: XN01 (Xét nghiệm máu), SA01 (Siêu âm)
     */
    private String maDichVu;

    /**
     * 4. MA_CHI_SO - Mã chỉ số xét nghiệm
     * Theo Phụ lục 11 QĐ 7603/QĐ-BYT
     * VD: GLUCOSE, WBC, RBC
     */
    private String maChiSo;

    /**
     * 5. TEN_CHI_SO - Tên chỉ số
     * VD: Đường huyết, Bạch cầu, Hồng cầu
     */
    private String tenChiSo;

    /**
     * 6. GIA_TRI - Giá trị kết quả
     * VD: "7.5", "8500"
     * 
     * ⚠️ Để null nếu chưa có kết quả
     * Cho phép bổ sung sau trong 30 ngày
     */
    private String giaTri;

    /**
     * 7. DON_VI_DO - Đơn vị đo
     * VD: "mmol/L", "cells/μL", "mg/dL"
     * Để null nếu chỉ số không có đơn vị đo
     */
    private String donViDo;

    /**
     * 8. MO_TA - Mô tả của người đọc kết quả
     * Để null nếu chưa có kết quả
     */
    private String moTa;

    /**
     * 9. KET_LUAN - Kết luận của người đọc kết quả
     * VD: "Bình thường", "Tăng cao", "Cần theo dõi"
     * Để null nếu chưa có kết quả
     */
    private String ketLuan;

    /**
     * 10. NGAY_KQ - Thời điểm có kết quả (yyyyMMddHHmm)
     * 
     * ⚠️ Để null nếu chưa có kết quả
     * Cho phép bổ sung sau trong 30 ngày
     */
    private String ngayKq;

    /**
     * 11. MA_BS_DOC_KQ - Mã bác sĩ đọc kết quả
     * Mã định danh y tế của người đọc/duyệt kết quả
     * Để null nếu chưa có người đọc
     */
    private String maBsDocKq;

    /**
     * 12. DU_PHONG - Dự phòng
     * Để null
     */
    private String duPhong;
}
