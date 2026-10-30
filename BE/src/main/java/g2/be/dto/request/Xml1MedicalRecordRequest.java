package g2.be.dto.request;

import lombok.*;
import java.math.BigDecimal;

/**
 * DTO cho yêu cầu tạo/cập nhật hồ sơ XML 1 (Bảng tổng hợp KBCB)
 * 
 * Thiết kế thông minh:
 * - 15 trường cốt lõi: Lấy từ DB thực tế (Invoice, MedicalRecord, Patient)
 * - 50 trường còn lại: Gán mặc định phù hợp với phòng khám ngoại trú
 * 
 * Tuân thủ 100% QĐ 130/QĐ-BYT mà không làm phình to database
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Xml1MedicalRecordRequest {

    // ==================== 15 TRƯỜNG CỐT LÕI (Lấy từ DB thực) ====================
    
    /**
     * 1. MA_LK - Mã đợt điều trị duy nhất (PRIMARY KEY)
     * Mapping: Invoice.invoiceId hoặc MedicalRecord.recordId
     */
    private String maLk;

    /**
     * 2. STT - Số thứ tự
     * Mapping: Tự tăng khi gửi batch, bắt đầu từ 1
     */
    private Integer stt;

    /**
     * 3. MA_BN - Mã bệnh nhân
     * Mapping: Patient.patientId
     */
    private String maBn;

    /**
     * 4. HO_TEN - Họ tên
     * Mapping: User.fullName (qua Patient.user)
     */
    private String hoTen;

    /**
     * 5. SO_CCCD - Số CCCD
     * Mapping: User.cccd
     */
    private String soCccd;

    /**
     * 6. NGAY_SINH - Ngày sinh (yyyyMMddHHMM)
     * Mapping: Patient.dateOfBirth → convert sang format String
     */
    private String ngaySinh;

    /**
     * 7. GIOI_TINH - Giới tính (1=Nam, 2=Nữ)
     * Mapping: User.gender
     */
    private Integer gioiTinh;

    /**
     * 16. MA_THE_BHYT - Mã thẻ BHYT
     * Mapping: Patient.healthInsuranceNumber (từ API tra cứu)
     */
    private String maTheBhyt;

    /**
     * 34. NGAY_VAO - Ngày vào khám (yyyyMMddHHMM)
     * Mapping: Appointment.appointmentDate + appointmentTime
     */
    private String ngayVao;

    /**
     * 36. NGAY_RA - Ngày ra viện (yyyyMMddHHMM)
     * Mapping: Invoice.paidAt (thời điểm thanh toán = kết thúc)
     */
    private String ngayRa;

    /**
     * 43. NGAY_TTOAN - Ngày thanh toán (yyyyMMddHHMM)
     * Mapping: Invoice.paidAt
     */
    private String ngayTtoan;

    /**
     * 46. T_TONGCHI_BV - Tổng chi phí bệnh viện
     * Mapping: Invoice.totalServiceFee
     */
    private BigDecimal tTongchiBv;

    /**
     * 47. T_TONGCHI_BH - Tổng chi phí BHYT thanh toán
     * Mapping: Invoice.insurancePaid
     */
    private BigDecimal tTongchiBh;

    /**
     * 48. T_BNTT - Tiền bệnh nhân tự trả
     * Mapping: Invoice.customerPaid
     */
    private BigDecimal tBntt;

    /**
     * 49. T_BNCCT - Tiền bệnh nhân cùng chi trả (20% nếu hưởng 80%)
     * Mapping: Tính toán dựa vào coverage_rate từ MockCitizenData
     */
    private BigDecimal tBncct;

    // ==================== 50 TRƯỜNG CÒN LẠI (Để null nếu không có data) ====================

    /**
     * 8. MA_QUOCTICH - Mã quốc tịch
     * Để null nếu không thu thập
     */
    private Integer maQuocTich;

    /**
     * 9. MA_DANTOC - Mã dân tộc
     * Để null nếu không thu thập
     */
    private Integer maDanToc;

    /**
     * 10. MA_NGHE_NGHIEP - Mã nghề nghiệp
     * Để null nếu không thu thập
     */
    private String maNgheNghiep;

    /**
     * 11. DIA_CHI - Địa chỉ
     * Lấy từ Patient.address (nếu có)
     */
    private String diaChi;

    /**
     * 12-14. Mã tỉnh/huyện/xã cư trú
     * Mặc định: Rỗng (phòng khám không thu thập chi tiết)
     */
    private String maTinhCuTru;
    private String maHuyenCuTru;
    private String maXaCuTru;

    /**
     * 15. DIEN_THOAI - Số điện thoại
     * Lấy từ User.phone (nếu có)
     */
    private String dienThoai;

    /**
     * 17-20. Thông tin thẻ BHYT chi tiết
     * Lấy từ MockCitizenData hoặc để rỗng
     */
    private String maDkbd;           // Mã CSKCB đăng ký ban đầu
    private String gtTheTu;          // Ngày bắt đầu hiệu lực
    private String gtTheDen;         // Ngày hết hạn
    private String ngayMienCct;      // Ngày miễn cùng chi trả (nếu có)

    /**
     * 21. LY_DO_VV - Lý do vào viện
     * Lấy từ MedicalRecord hoặc để null
     */
    private String lyDoVv;

    /**
     * 22-23. Lý do vào nội trú
     * Để null (không áp dụng cho ngoại trú)
     */
    private String lyDoVnt;
    private String maLyDoVnt;

    /**
     * 24. CHAN_DOAN_VAO - Chẩn đoán vào
     * Lấy từ MedicalRecord.symptoms
     */
    private String chanDoanVao;

    /**
     * 25. CHAN_DOAN_RV - Chẩn đoán ra viện
     * Lấy từ MedicalRecord.diagnosis
     */
    private String chanDoanRv;

    /**
     * 26-27. Mã bệnh ICD-10
     * Lấy từ MedicalRecord (nếu có phân loại ICD-10)
     */
    private String maBenhChinh;
    private String maBenhKt;

    /**
     * 28-29. Mã bệnh YHCT và phẫu thuật
     * Mặc định: Rỗng (phòng khám không làm YHCT/phẫu thuật)
     */
    private String maBenhYhct;
    private String maPtttQt;

    /**
     * 30. MA_DOITUONG_KCB - Mã đối tượng khám
     * Để null hoặc mapper sẽ xác định dựa vào có BHYT hay không
     */
    private String maDoiTuongKcb;

    /**
     * 31-32. Chuyển tuyến
     * Để null (không chuyển tuyến)
     */
    private String maNoiDi;
    private String maNoiDen;

    /**
     * 33. MA_TAI_NAN - Mã tai nạn
     * Để null (không tai nạn)
     */
    private Integer maTaiNan;

    /**
     * 35. NGAY_VAO_NOI_TRU - Ngày vào nội trú
     * Để null (phòng khám ngoại trú)
     */
    private String ngayVaoNoiTru;

    /**
     * 37. GIAY_CHUYEN_TUYEN - Số giấy chuyển tuyến
     * Để null
     */
    private String giayChuyenTuyen;

    /**
     * 38. SO_NGAY_DTRI - Số ngày điều trị
     * Để null hoặc mapper tính toán
     */
    private Integer soNgayDtri;

    /**
     * 39. PP_DIEU_TRI - Phương pháp điều trị
     * Lấy từ MedicalRecord.doctorNotes
     */
    private String ppDieuTri;

    /**
     * 40. KET_QUA_DTRI - Kết quả điều trị
     * Để null hoặc mapper xác định
     */
    private Integer ketQuaDtri;

    /**
     * 41. MA_LOAI_RV - Mã loại ra viện
     * Để null hoặc mapper xác định
     */
    private Integer maLoaiRv;

    /**
     * 42. GHI_CHU - Ghi chú
     * Lấy từ MedicalRecord.doctorNotes
     */
    private String ghiChu;

    /**
     * 44-45. Tiền thuốc và vật tư
     * Để null (phòng khám không bán thuốc) hoặc lấy từ DB nếu có
     */
    private BigDecimal tThuoc;
    private BigDecimal tVtyt;

    /**
     * 50. T_BHTT - Tiền BHXH thanh toán
     * Công thức: T_TONGCHI_BH - T_BNCCT
     */
    private BigDecimal tBhtt;

    /**
     * 51-52. Nguồn khác và ngoài định suất
     * Để null
     */
    private BigDecimal tNguonkhac;
    private BigDecimal tBhttGdv;

    /**
     * 53-54. Năm/tháng quyết toán
     * Lấy từ thời điểm thanh toán
     */
    private Integer namQt;
    private Integer thangQt;

    /**
     * 55. MA_LOAI_KCB - Mã loại khám
     * Để null hoặc mapper xác định (1: Khám bệnh, 2: Ngoại trú...)
     */
    private Integer maLoaiKcb;

    /**
     * 56. MA_KHOA - Mã khoa
     * Để null hoặc lấy từ Doctor.department
     */
    private String maKhoa;

    /**
     * 57. MA_CSKCB - Mã cơ sở khám
     * Lấy từ config hoặc để null
     */
    private String maCskcb;

    /**
     * 58. MA_KHUVUC - Mã khu vực
     * Để null (sẽ được xác định qua địa chỉ nếu cần)
     */
    private String maKhuvuc;

    /**
     * 59. CAN_NANG - Cân nặng
     * Lấy từ Patient.weight (nếu có)
     */
    private String canNang;

    /**
     * 60. CAN_NANG_CON - Cân nặng con
     * Mặc định: Rỗng (không sinh con)
     */
    private String canNangCon;

    /**
     * 61. NAM_NAM_LIEN_TUC - Tham gia BHYT 5 năm liên tục
     * Mặc định: Rỗng
     */
    private String namNamLienTuc;

    /**
     * 62. NGAY_TAI_KHAM - Ngày tái khám
     * Mặc định: Rỗng
     */
    private String ngayTaiKham;

    /**
     * 63. MA_HSBA - Mã hồ sơ bệnh án
     * Mapping: MedicalRecord.recordId
     */
    private String maHsba;

    /**
     * 64. MA_TTDV - Mã người đứng đầu
     * Lấy từ config phòng khám hoặc để null
     */
    private String maTtdv;

    /**
     * 65. DU_PHONG - Dự phòng
     * Để null
     */
    private String duPhong;
}
