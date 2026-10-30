package g2.be.service;

import g2.be.config.ClinicInsuranceConfig;
import g2.be.dto.response.CitizenInsuranceInfoResponse;
import g2.be.dto.response.InsuranceBreakdownResponse;
import g2.be.entity.Appointment;
import g2.be.entity.MockCitizenData;
import g2.be.entity.MockServiceCatalog;
import g2.be.exception.AppException;
import g2.be.exception.ErrorCode;
import g2.be.repository.AppointmentRepository;
import g2.be.repository.MockCitizenDataRepository;
import g2.be.repository.MockServiceCatalogRepository;
import g2.be.repository.ServiceOrderRepository;
import g2.be.util.ClinicTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service tra cứu thông tin công dân và thẻ BHYT
 * + Tính toán BHYT cho từng dịch vụ trong lịch hẹn
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CitizenInsuranceService {

    private final MockCitizenDataRepository mockCitizenDataRepository;
    private final MockServiceCatalogRepository mockServiceCatalogRepository;
    private final AppointmentRepository appointmentRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final ClinicInsuranceConfig clinicConfig;

    /**
     * Tra cứu thông tin công dân theo số CCCD
     */
    public CitizenInsuranceInfoResponse getCitizenInfoByCccd(String cccdNumber) {
        MockCitizenData citizen = mockCitizenDataRepository.findByCccdNumber(cccdNumber)
            .orElseThrow(() -> new AppException(ErrorCode.INSURANCE_NOT_FOUND));

        return buildResponse(citizen);
    }

    /**
     * Tra cứu thông tin công dân theo mã thẻ BHYT
     */
    public CitizenInsuranceInfoResponse getCitizenInfoByInsuranceCode(String insuranceCode) {
        MockCitizenData citizen = mockCitizenDataRepository.findByInsuranceCode(insuranceCode)
            .orElseThrow(() -> new RuntimeException(
                "Không tìm thấy thông tin thẻ BHYT: " + insuranceCode + 
                ". Vui lòng kiểm tra lại mã thẻ."
            ));

        return buildResponse(citizen);
    }

    /**
     * Tính toán BHYT chi tiết cho từng dịch vụ trong lịch hẹn
     * 
     * Công thức:
     * - Giá_Cơ_Sở = min(Giá_Phòng_Khám, Giá_Trần_BHYT)
     * - Tiền_BHYT_Trả = Giá_Cơ_Sở × (Mức_Hưởng_Thẻ / 100) × (Tỷ_Lệ_Dịch_Vụ / 100)
     * - Tiền_Bệnh_Nhân_Trả = Giá_Phòng_Khám − Tiền_BHYT_Trả
     *
     * @param cccdNumber Số CCCD để tra cứu thông tin BHYT
     * @param appointmentId Mã lịch hẹn để lấy danh sách dịch vụ
     * @return InsuranceBreakdownResponse với chi tiết BHYT từng dịch vụ
     */
    public InsuranceBreakdownResponse calculateInsuranceForAppointment(String cccdNumber, String appointmentId) {
        // 1. Tra cứu thông tin công dân
        MockCitizenData citizen = mockCitizenDataRepository.findByCccdNumber(cccdNumber)
            .orElseThrow(() -> new AppException(ErrorCode.INSURANCE_NOT_FOUND));

        CitizenInsuranceInfoResponse citizenInfo = buildResponse(citizen);

        // 2. Kiểm tra thẻ BHYT còn hiệu lực
        if ("EXPIRED".equals(citizenInfo.getStatus())) {
            throw new AppException(ErrorCode.INSURANCE_EXPIRED);
        }

        // 3. Lấy appointment và danh sách dịch vụ
        Appointment appointment = appointmentRepository.findById(appointmentId)
            .orElseThrow(() -> new AppException(ErrorCode.APPOINTMENT_NOT_FOUND));

        List<g2.be.entity.Service> services = appointment.getServices();
        
        // 4. Tính toán BHYT cho từng dịch vụ
        Integer finalCoverageRate = citizenInfo.getFinalCoverageRate(); // Mức hưởng thẻ (đã tính tuyến)
        
        List<InsuranceBreakdownResponse.ServiceInsuranceDetail> breakdowns = new ArrayList<>();
        BigDecimal totalClinicPrice = BigDecimal.ZERO;
        BigDecimal totalInsurancePaid = BigDecimal.ZERO;
        BigDecimal totalPatientPaid = BigDecimal.ZERO;

        for (g2.be.entity.Service service : services) {
            BigDecimal clinicPrice = service.getPrice(); // Giá phòng khám
            totalClinicPrice = totalClinicPrice.add(clinicPrice);

            // Tra cứu giá trần BHYT theo bhyt_service_code (mapping với mock_service_catalog.service_code)
            String bhytCode = service.getBhytServiceCode();
            var catalogOpt = (bhytCode != null && !bhytCode.isEmpty())
                ? mockServiceCatalogRepository.findByServiceCodeAndIsActiveTrue(bhytCode)
                : java.util.Optional.<MockServiceCatalog>empty();

            InsuranceBreakdownResponse.ServiceInsuranceDetail detail;

            if (catalogOpt.isPresent()) {
                MockServiceCatalog catalog = catalogOpt.get();
                BigDecimal ceilingPrice = catalog.getCeilingPrice();
                Integer serviceCoverageRatio = catalog.getCoverageRatio();

                // Công thức BHYT
                // Giá_Cơ_Sở = min(Giá_Phòng_Khám, Giá_Trần_BHYT)
                BigDecimal basePrice = clinicPrice.min(ceilingPrice);

                // Tiền_BHYT_Trả = Giá_Cơ_Sở × (Mức_Hưởng_Thẻ / 100) × (Tỷ_Lệ_Dịch_Vụ / 100)
                BigDecimal insurancePaid = basePrice
                    .multiply(BigDecimal.valueOf(finalCoverageRate))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(serviceCoverageRatio))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                // Tiền_Bệnh_Nhân_Trả = Giá_Phòng_Khám − Tiền_BHYT_Trả
                BigDecimal patientPaid = clinicPrice.subtract(insurancePaid);

                totalInsurancePaid = totalInsurancePaid.add(insurancePaid);
                totalPatientPaid = totalPatientPaid.add(patientPaid);

                detail = InsuranceBreakdownResponse.ServiceInsuranceDetail.builder()
                    .serviceId(service.getServiceId())
                    .serviceName(service.getServiceName())
                    .clinicPrice(clinicPrice)
                    .ceilingPrice(ceilingPrice)
                    .basePrice(basePrice)
                    .cardCoverageRate(finalCoverageRate)
                    .serviceCoverageRatio(serviceCoverageRatio)
                    .insurancePaid(insurancePaid)
                    .patientPaid(patientPaid)
                    .isCoveredByInsurance(true)
                    .build();

                log.info("Service {} [{}]: clinicPrice={}, ceilingPrice={}, basePrice={}, " +
                         "cardRate={}%, serviceRatio={}%, insurancePaid={}, patientPaid={}",
                    service.getServiceId(), service.getServiceName(),
                    clinicPrice, ceilingPrice, basePrice,
                    finalCoverageRate, serviceCoverageRatio,
                    insurancePaid, patientPaid);
            } else {
                // Dịch vụ ngoài danh mục BHYT → bệnh nhân trả 100%
                totalPatientPaid = totalPatientPaid.add(clinicPrice);

                detail = InsuranceBreakdownResponse.ServiceInsuranceDetail.builder()
                    .serviceId(service.getServiceId())
                    .serviceName(service.getServiceName())
                    .clinicPrice(clinicPrice)
                    .ceilingPrice(null)
                    .basePrice(BigDecimal.ZERO)
                    .cardCoverageRate(finalCoverageRate)
                    .serviceCoverageRatio(0)
                    .insurancePaid(BigDecimal.ZERO)
                    .patientPaid(clinicPrice)
                    .isCoveredByInsurance(false)
                    .build();

                log.info("Service {} [{}]: Ngoài danh mục BHYT, bệnh nhân trả 100%: {}",
                    service.getServiceId(), service.getServiceName(), clinicPrice);
            }

            breakdowns.add(detail);
        }

        log.info("=== BHYT Summary for CCCD {} ===", cccdNumber);
        log.info("Total Clinic Price: {}, Insurance Paid: {}, Patient Paid: {}",
            totalClinicPrice, totalInsurancePaid, totalPatientPaid);

        return InsuranceBreakdownResponse.builder()
            .citizenInfo(citizenInfo)
            .serviceBreakdowns(breakdowns)
            .totalClinicPrice(totalClinicPrice)
            .totalInsurancePaid(totalInsurancePaid)
            .totalPatientPaid(totalPatientPaid)
            .build();
    }

    /**
     * Tính toán BHYT cho danh sách dịch vụ cụ thể (dùng cho supplement payment)
     *
     * @param cccdNumber Số CCCD để tra cứu BHYT
     * @param services   Danh sách dịch vụ cần tính
     */
    public InsuranceBreakdownResponse calculateInsuranceForServices(String cccdNumber, List<g2.be.entity.Service> services) {
        MockCitizenData citizen = mockCitizenDataRepository.findByCccdNumber(cccdNumber)
            .orElseThrow(() -> new AppException(ErrorCode.INSURANCE_NOT_FOUND));

        CitizenInsuranceInfoResponse citizenInfo = buildResponse(citizen);

        if ("EXPIRED".equals(citizenInfo.getStatus())) {
            throw new AppException(ErrorCode.INSURANCE_EXPIRED);
        }

        Integer finalCoverageRate = citizenInfo.getFinalCoverageRate();

        List<InsuranceBreakdownResponse.ServiceInsuranceDetail> breakdowns = new ArrayList<>();
        BigDecimal totalClinicPrice = BigDecimal.ZERO;
        BigDecimal totalInsurancePaid = BigDecimal.ZERO;
        BigDecimal totalPatientPaid = BigDecimal.ZERO;

        for (g2.be.entity.Service service : services) {
            BigDecimal clinicPrice = service.getPrice();
            totalClinicPrice = totalClinicPrice.add(clinicPrice);

            String bhytCode = service.getBhytServiceCode();
            var catalogOpt = (bhytCode != null && !bhytCode.isEmpty())
                ? mockServiceCatalogRepository.findByServiceCodeAndIsActiveTrue(bhytCode)
                : java.util.Optional.<MockServiceCatalog>empty();

            InsuranceBreakdownResponse.ServiceInsuranceDetail detail;

            if (catalogOpt.isPresent()) {
                MockServiceCatalog catalog = catalogOpt.get();
                BigDecimal ceilingPrice = catalog.getCeilingPrice();
                Integer serviceCoverageRatio = catalog.getCoverageRatio();

                BigDecimal basePrice = clinicPrice.min(ceilingPrice);
                BigDecimal insurancePaid = basePrice
                    .multiply(BigDecimal.valueOf(finalCoverageRate))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(serviceCoverageRatio))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                BigDecimal patientPaid = clinicPrice.subtract(insurancePaid);

                totalInsurancePaid = totalInsurancePaid.add(insurancePaid);
                totalPatientPaid = totalPatientPaid.add(patientPaid);

                detail = InsuranceBreakdownResponse.ServiceInsuranceDetail.builder()
                    .serviceId(service.getServiceId())
                    .serviceName(service.getServiceName())
                    .clinicPrice(clinicPrice)
                    .ceilingPrice(ceilingPrice)
                    .basePrice(basePrice)
                    .cardCoverageRate(finalCoverageRate)
                    .serviceCoverageRatio(serviceCoverageRatio)
                    .insurancePaid(insurancePaid)
                    .patientPaid(patientPaid)
                    .isCoveredByInsurance(true)
                    .build();
            } else {
                totalPatientPaid = totalPatientPaid.add(clinicPrice);

                detail = InsuranceBreakdownResponse.ServiceInsuranceDetail.builder()
                    .serviceId(service.getServiceId())
                    .serviceName(service.getServiceName())
                    .clinicPrice(clinicPrice)
                    .ceilingPrice(null)
                    .basePrice(BigDecimal.ZERO)
                    .cardCoverageRate(finalCoverageRate)
                    .serviceCoverageRatio(0)
                    .insurancePaid(BigDecimal.ZERO)
                    .patientPaid(clinicPrice)
                    .isCoveredByInsurance(false)
                    .build();
            }

            breakdowns.add(detail);
        }

        return InsuranceBreakdownResponse.builder()
            .citizenInfo(citizenInfo)
            .serviceBreakdowns(breakdowns)
            .totalClinicPrice(totalClinicPrice)
            .totalInsurancePaid(totalInsurancePaid)
            .totalPatientPaid(totalPatientPaid)
            .build();
    }

    /**
     * Tra cứu BHYT và tính toán chi tiết cho các dịch vụ SUPPLEMENT đang chờ thanh toán của một hồ sơ khám.
     * Dùng cho thu ngân tra cứu BHYT trước khi thu tiền bổ sung.
     */
    public InsuranceBreakdownResponse calculateInsuranceForRecord(String cccdNumber, String recordId) {
        List<g2.be.entity.ServiceOrder> pendingOrders = serviceOrderRepository
                .findByMedicalRecord_RecordIdAndOrderTypeAndStatus(recordId, "SUPPLEMENT", "PENDING");

        if (pendingOrders.isEmpty()) {
            throw new AppException(ErrorCode.ORDER_NOT_FOUND);
        }

        List<g2.be.entity.Service> services = pendingOrders.stream()
                .map(g2.be.entity.ServiceOrder::getService)
                .toList();

        return calculateInsuranceForServices(cccdNumber, services);
    }

    /**
     * Chuyển đổi Entity sang DTO Response với validation và tính toán trái tuyến
     */
    private CitizenInsuranceInfoResponse buildResponse(MockCitizenData citizen) {
        LocalDate today = ClinicTime.today();
        boolean isExpired = citizen.getValidTo() != null && citizen.getValidTo().isBefore(today);

        // Lấy mã nhóm đối tượng (2 ký tự đầu: TE, QN, GD, HT...)
        String beneficiaryType = extractBeneficiaryType(citizen.getInsuranceCode());
        int beneficiaryGroup = determineBeneficiaryGroup(beneficiaryType);
        
        // Lấy mã tỉnh từ 2 số đầu mã thẻ BHYT (VD: GD4010112345678 -> 40)
        String provinceCode = extractProvinceCode(citizen.getInsuranceCode());
        
        // Tính toán mức hưởng gốc từ ký tự thứ 3 của mã thẻ
        Integer originalCoverageRate = calculateOriginalCoverageRate(citizen.getInsuranceCode());
        
        // NHÓM 1: "Kim bài miễn tử" - Bỏ qua hoàn toàn kiểm tra tuyến
        boolean isDungTuyen;
        Integer tuyenRatio;
        Integer finalCoverageRate;
        
        if (beneficiaryGroup == 1) {
            // Nhóm ưu tiên tuyệt đối: TE, QN, CA, CY, XD, DT
            isDungTuyen = true;  // Luôn coi như đúng tuyến
            tuyenRatio = 100;     // Luôn 100%
            finalCoverageRate = 100; // Bắt buộc 100% bất kể ký tự thứ 3
        } else {
            // NHÓM 2 & 3: Dân thường, Người lao động, Hưu trí - Phải kiểm tra tuyến
            isDungTuyen = checkDungTuyen(citizen.getMaDkbd(), provinceCode);
            tuyenRatio = calculateTuyenRatio(isDungTuyen);
            finalCoverageRate = (originalCoverageRate * tuyenRatio) / 100;
        }

        String status = isExpired ? "EXPIRED" : "ACTIVE";
        
        // Tạo message chi tiết
        String message = buildMessage(isExpired, isDungTuyen, originalCoverageRate, finalCoverageRate, beneficiaryType);

        return CitizenInsuranceInfoResponse.builder()
            .cccdNumber(citizen.getCccdNumber())
            .insuranceCode(citizen.getInsuranceCode())
            .fullName(citizen.getFullName())
            .dateOfBirth(citizen.getDateOfBirth())
            .gender(citizen.getGender())
            .genderName(citizen.getGender() == 1 ? "Nam" : citizen.getGender() == 2 ? "Nữ" : "Không xác định")
            .address(citizen.getAddress())
            .coverageRate(citizen.getCoverageRate()) // Giữ lại cho backward compatibility
            .maDkbd(citizen.getMaDkbd())
            .benefitLevel(citizen.getBenefitLevel())
            .provinceCode(provinceCode)
            .isDungTuyen(isDungTuyen)
            .tuyenRatio(tuyenRatio)
            .originalCoverageRate(originalCoverageRate)
            .finalCoverageRate(finalCoverageRate)
            .validTo(citizen.getValidTo())
            .status(status)
            .message(message)
            .build();
    }
    
    /**
     * Trích xuất mã nhóm đối tượng (2 ký tự đầu tiên)
     */
    private String extractBeneficiaryType(String insuranceCode) {
        if (insuranceCode == null || insuranceCode.length() < 2) {
            return "XX";
        }
        return insuranceCode.substring(0, 2).toUpperCase();
    }
    
    /**
     * Xác định nhóm ưu tiên dựa trên 2 ký tự đầu
     */
    private int determineBeneficiaryGroup(String beneficiaryType) {
        // Nhóm 1: Kim bài miễn tử
        if ("TE".equals(beneficiaryType) ||  // Trẻ em dưới 6 tuổi
            "QN".equals(beneficiaryType) ||  // Quân nhân
            "CA".equals(beneficiaryType) ||  // Công an
            "CY".equals(beneficiaryType) ||  // Cựu chiến binh
            "XD".equals(beneficiaryType) ||  // Xã đảo, biên giới
            "DT".equals(beneficiaryType)) {  // Dân tộc thiểu số vùng khó khăn
            return 1;
        }
        
        // Nhóm 3: Hưu trí & Có công (vẫn phải xét tuyến)
        if ("HT".equals(beneficiaryType) ||  // Hưu trí
            "CC".equals(beneficiaryType) ||  // Có công với cách mạng
            "CK".equals(beneficiaryType) ||  // Con liệt sĩ, thương binh
            "BT".equals(beneficiaryType)) {  // Bảo trợ xã hội
            return 3;
        }
        
        // Nhóm 2: Dân thường & Người lao động (mặc định)
        return 2;
    }
    
    /**
     * Trích xuất mã tỉnh từ mã thẻ BHYT (2 số ở vị trí 3-4)
     */
    private String extractProvinceCode(String insuranceCode) {
        if (insuranceCode == null || insuranceCode.length() < 4) {
            return "00";
        }
        return insuranceCode.substring(2, 4);
    }
    
    /**
     * Tính mức hưởng gốc dựa vào ký tự thứ 3 của mã thẻ
     */
    private Integer calculateOriginalCoverageRate(String insuranceCode) {
        if (insuranceCode == null || insuranceCode.length() < 3) {
            return 80; // Mặc định
        }
        
        char thirdChar = insuranceCode.charAt(2);
        
        switch (thirdChar) {
            case '1':
            case '2':
                return 100;
            case '3':
                return 95;
            case '4':
                return 80;
            default:
                return 80;
        }
    }
    
    /**
     * Kiểm tra đúng tuyến/trái tuyến
     */
    private boolean checkDungTuyen(String maDkbd, String patientProvinceCode) {
        // Trường hợp 1: Đúng nơi đăng ký ban đầu
        if (maDkbd != null && maDkbd.equals(clinicConfig.getMaCskcb())) {
            return true;
        }
        
        // Trường hợp 2: Cùng tỉnh (Thông tuyến huyện)
        if (patientProvinceCode != null && patientProvinceCode.equals(clinicConfig.getProvinceCode())) {
            return true;
        }
        
        // Trường hợp 3: Khác tỉnh
        return false;
    }
    
    /**
     * Tính tỷ lệ tuyến (Route Multiplier)
     */
    private Integer calculateTuyenRatio(boolean isDungTuyen) {
        return isDungTuyen ? 100 : 0;
    }
    
    /**
     * Tạo message thông báo chi tiết
     */
    private String buildMessage(boolean isExpired, boolean isDungTuyen, 
                                Integer originalRate, Integer finalRate, String beneficiaryType) {
        if (isExpired) {
            return "⚠️ Thẻ BHYT đã hết hạn. Vui lòng gia hạn thẻ trước khi khám.";
        }
        
        return String.format("BHYT chi trả %d%%", finalRate);
    }
}
