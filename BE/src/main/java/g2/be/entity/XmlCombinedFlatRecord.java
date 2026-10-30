package g2.be.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "xml_combined_flat_record")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(XmlCombinedFlatRecordId.class)
public class XmlCombinedFlatRecord {

    // ==================== THÔNG TIN CA KHÁM (Từ XML1) ====================
    @Id
    @Column(name = "ma_lk", length = 100)
    private String maLk;

    @Column(name = "stt_xml1")
    private Integer sttXml1;

    @Column(name = "ma_bn", length = 100)
    private String maBn;

    @Column(name = "ho_ten", length = 255)
    private String hoTen;

    @Column(name = "so_cccd", length = 20)
    private String soCccd;

    @Column(name = "ngay_sinh", length = 12)
    private String ngaySinh;

    @Column(name = "gioi_tinh")
    private Integer gioiTinh;

    @Column(name = "ma_quoc_tich")
    private Integer maQuocTich;

    @Column(name = "ma_dan_toc")
    private Integer maDanToc;

    @Column(name = "ma_nghe_nghiep", length = 50)
    private String maNgheNghiep;

    @Column(name = "dia_chi", length = 500)
    private String diaChi;

    @Column(name = "ma_tinh_cu_tru", length = 10)
    private String maTinhCuTru;

    @Column(name = "ma_huyen_cu_tru", length = 10)
    private String maHuyenCuTru;

    @Column(name = "ma_xa_cu_tru", length = 10)
    private String maXaCuTru;

    @Column(name = "dien_thoai", length = 20)
    private String dienThoai;

    @Column(name = "ma_the_bhyt", length = 50)
    private String maTheBhyt;

    @Column(name = "ma_dkbd", length = 50)
    private String maDkbd;

    @Column(name = "gt_the_tu", length = 12)
    private String gtTheTu;

    @Column(name = "gt_the_den", length = 12)
    private String gtTheDen;

    @Column(name = "ngay_mien_cct", length = 12)
    private String ngayMienCct;

    @Column(name = "ly_do_vv", length = 500)
    private String lyDoVv;

    @Column(name = "ly_do_vnt", length = 500)
    private String lyDoVnt;

    @Column(name = "ma_ly_do_vnt", length = 50)
    private String maLyDoVnt;

    @Column(name = "chan_doan_vao", length = 500)
    private String chanDoanVao;

    @Column(name = "chan_doan_rv", length = 500)
    private String chanDoanRv;

    @Column(name = "ma_benh_chinh", length = 20)
    private String maBenhChinh;

    @Column(name = "ma_benh_kt", length = 200)
    private String maBenhKt;

    @Column(name = "ma_benh_yhct", length = 200)
    private String maBenhYhct;

    @Column(name = "ma_pttt_qt", length = 200)
    private String maPtttQt;

    @Column(name = "ma_doi_tuong_kcb", length = 10)
    private String maDoiTuongKcb;

    @Column(name = "ma_noi_di", length = 50)
    private String maNoiDi;

    @Column(name = "ma_noi_den", length = 50)
    private String maNoiDen;

    @Column(name = "ma_tai_nan")
    private Integer maTaiNan;

    @Column(name = "ngay_vao", length = 12)
    private String ngayVao;

    @Column(name = "ngay_vao_noi_tru", length = 12)
    private String ngayVaoNoiTru;

    @Column(name = "ngay_ra", length = 12)
    private String ngayRa;

    @Column(name = "giay_chuyen_tuyen", length = 50)
    private String giayChuyenTuyen;

    @Column(name = "so_ngay_dtri")
    private Integer soNgayDtri;

    @Column(name = "pp_dieu_tri", length = 500)
    private String ppDieuTri;

    @Column(name = "ket_qua_dtri")
    private Integer ketQuaDtri;

    @Column(name = "ma_loai_rv")
    private Integer maLoaiRv;

    @Column(name = "ghi_chu", length = 1000)
    private String ghiChu;

    @Column(name = "ngay_ttoan", length = 12)
    private String ngayTtoan;

    @Column(name = "t_thuoc", precision = 18, scale = 2)
    private BigDecimal tThuoc;

    @Column(name = "t_vtyt", precision = 18, scale = 2)
    private BigDecimal tVtyt;

    @Column(name = "t_tongchi_bv", precision = 18, scale = 2)
    private BigDecimal tTongchiBv;

    @Column(name = "t_tongchi_bh", precision = 18, scale = 2)
    private BigDecimal tTongchiBh;

    @Column(name = "t_bntt", precision = 18, scale = 2)
    private BigDecimal tBntt;

    @Column(name = "t_bncct", precision = 18, scale = 2)
    private BigDecimal tBncct;

    @Column(name = "t_bhtt", precision = 18, scale = 2)
    private BigDecimal tBhtt;

    @Column(name = "t_nguonkhac", precision = 18, scale = 2)
    private BigDecimal tNguonkhac;

    @Column(name = "t_bhtt_gdv", precision = 18, scale = 2)
    private BigDecimal tBhttGdv;

    @Column(name = "nam_qt")
    private Integer namQt;

    @Column(name = "thang_qt")
    private Integer thangQt;

    @Column(name = "ma_loai_kcb")
    private Integer maLoaiKcb;

    @Column(name = "ma_khoa", length = 50)
    private String maKhoa;

    @Column(name = "ma_cskcb", length = 50)
    private String maCskcb;

    @Column(name = "ma_khuvuc", length = 10)
    private String maKhuvuc;

    @Column(name = "can_nang", length = 20)
    private String canNang;

    @Column(name = "can_nang_con", length = 20)
    private String canNangCon;

    @Column(name = "nam_nam_lien_tuc", length = 10)
    private String namNamLienTuc;

    @Column(name = "ngay_tai_kham", length = 12)
    private String ngayTaiKham;

    @Column(name = "ma_hsba", length = 100)
    private String maHsba;

    @Column(name = "ma_ttdv", length = 100)
    private String maTtdv;

    @Column(name = "du_phong_xml1", length = 500)
    private String duPhongXml1;

    // ==================== CHI TIẾT CẬN LÂM SÀNG (Từ XML4) ====================
    @Id
    @Column(name = "stt_xml4")
    private Integer sttXml4;

    @Column(name = "ma_dich_vu", length = 50)
    private String maDichVu;

    @Column(name = "ma_chi_so", length = 50)
    private String maChiSo;

    @Column(name = "ten_chi_so", length = 255)
    private String tenChiSo;

    @Column(name = "gia_tri", length = 255)
    private String giaTri;

    @Column(name = "don_vi_do", length = 50)
    private String donViDo;

    @Column(name = "mo_ta", length = 1000)
    private String moTa;

    @Column(name = "ket_luan", length = 500)
    private String ketLuan;

    @Column(name = "ngay_kq", length = 12)
    private String ngayKq;

    @Column(name = "ma_bs_doc_kq", length = 100)
    private String maBsDocKq;

    @Column(name = "du_phong_xml4", length = 500)
    private String duPhongXml4;

}
