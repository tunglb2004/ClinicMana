package g2.be.service;

import g2.be.dto.request.PrescriptionRequest;
import g2.be.dto.response.PrescriptionDetailResponse;
import g2.be.dto.response.PrescriptionResponse;
import g2.be.entity.MedicalRecord;
import g2.be.entity.Prescription;
import g2.be.entity.PrescriptionDetail;
import g2.be.exception.AppException;
import g2.be.exception.ErrorCode;
import g2.be.repository.MedicalRecordRepository;
import g2.be.repository.PrescriptionDetailRepository;
import g2.be.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionDetailRepository prescriptionDetailRepository;
    private final MedicalRecordRepository medicalRecordRepository;

    @Transactional
    public PrescriptionResponse issue(PrescriptionRequest request) {
        MedicalRecord medicalRecord = medicalRecordRepository.findById(request.getMedicalRecordId())
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND));

        if (!"EXAMINING".equals(medicalRecord.getStatus())) {
            throw new AppException(ErrorCode.MEDICAL_RECORD_NOT_EXAMINING);
        }

        String prescriptionId = "PRES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        Prescription prescription = Prescription.builder()
                .prescriptionId(prescriptionId)
                .medicalRecord(medicalRecord)
                .notes(request.getNotes())
                .build();

        Prescription savedPrescription = prescriptionRepository.save(prescription);

        List<PrescriptionDetail> details = request.getDetails().stream()
                .map(detailRequest -> PrescriptionDetail.builder()
                        .prescription(savedPrescription)
                        .medicineName(detailRequest.getMedicineName())
                        .dosage(detailRequest.getDosage())
                        .quantity(detailRequest.getQuantity())
                        // Note: current schema doesn't have frequency, duration, unit, note
                        .build())
                .collect(Collectors.toList());

        prescriptionDetailRepository.saveAll(details);
        savedPrescription.setDetails(details);

        return mapToResponse(savedPrescription);
    }

    @Transactional
    public PrescriptionResponse updateDetail(String id, PrescriptionRequest request) {
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRESCRIPTION_NOT_FOUND));

        MedicalRecord medicalRecord = prescription.getMedicalRecord();
        if ("FINISHED".equals(medicalRecord.getStatus()) || "CANCELLED".equals(medicalRecord.getStatus())) {
            throw new AppException(ErrorCode.MEDICAL_RECORD_ALREADY_FINISHED);
        }

        prescription.setNotes(request.getNotes());
        
        // Delete old details
        prescriptionDetailRepository.deleteByPrescription(prescription);
        
        // Save new details
        List<PrescriptionDetail> details = request.getDetails().stream()
                .map(detailRequest -> PrescriptionDetail.builder()
                        .prescription(prescription)
                        .medicineName(detailRequest.getMedicineName())
                        .dosage(detailRequest.getDosage())
                        .quantity(detailRequest.getQuantity())
                        .build())
                .collect(Collectors.toList());

        prescriptionDetailRepository.saveAll(details);
        prescription.getDetails().clear();
        prescription.getDetails().addAll(details);

        return mapToResponse(prescriptionRepository.save(prescription));
    }

    @Transactional(readOnly = true)
    public String generateExportDocument(String id) {
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRESCRIPTION_NOT_FOUND));
        
        MedicalRecord record = prescription.getMedicalRecord();
        
        // Placeholder for clinic info - in real app would come from config
        String clinicName = "PHÒNG KHÁM ĐA KHOA G2";
        String clinicAddress = "123 Đường HTML, Hòa Lạc, Hà Nội";

        StringBuilder html = new StringBuilder();
        html.append("<html><head><style>")
            .append("body { font-family: Arial, sans-serif; margin: 40px; }")
            .append(".header { text-align: center; margin-bottom: 30px; }")
            .append(".info { margin-bottom: 20px; }")
            .append("table { width: 100%; border-collapse: collapse; margin-top: 20px; }")
            .append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }")
            .append("th { background-color: #f2f2f2; }")
            .append(".footer { margin-top: 50px; text-align: right; }")
            .append("</style></head><body>");

        html.append("<div class='header'>")
            .append("<h1>").append(clinicName).append("</h1>")
            .append("<p>").append(clinicAddress).append("</p>")
            .append("<h2>ĐƠN THUỐC</h2>")
            .append("</div>");

        html.append("<div class='info'>")
            .append("<p><b>Bệnh nhân:</b> ").append(record.getPatient().getUser().getFullName()).append("</p>")
            .append("<p><b>Ngày sinh:</b> ").append(record.getPatient().getDateOfBirth()).append("</p>")
            .append("<p><b>Giới tính:</b> ").append(record.getPatient().getUser().getGender()).append("</p>")
            .append("<p><b>Bác sĩ kê đơn:</b> ").append(record.getDoctor().getUser().getFullName()).append("</p>")
            .append("<p><b>Chẩn đoán:</b> ").append(record.getDiagnosis()).append("</p>")
            .append("</div>");

        html.append("<table>")
            .append("<tr><th>STT</th><th>Tên thuốc</th><th>Liều dùng</th><th>Số lượng</th></tr>");

        int stt = 1;
        for (PrescriptionDetail detail : prescription.getDetails()) {
            html.append("<tr>")
                .append("<td>").append(stt++).append("</td>")
                .append("<td>").append(detail.getMedicineName()).append("</td>")
                .append("<td>").append(detail.getDosage() != null ? detail.getDosage() : "").append("</td>")
                .append("<td>").append(detail.getQuantity()).append("</td>")
                .append("</tr>");
        }
        html.append("</table>");

        if (prescription.getNotes() != null && !prescription.getNotes().isEmpty()) {
            html.append("<p><b>Ghi chú:</b> ").append(prescription.getNotes()).append("</p>");
        }

        html.append("<div class='footer'>")
            .append("<p>Ngày .... tháng .... năm ....</p>")
            .append("<p><b>Bác sĩ điều trị</b></p>")
            .append("<br><br><br>")
            .append("<p>(Ký và ghi rõ họ tên)</p>")
            .append("</div>");

        html.append("</body></html>");

        return html.toString();
    }

    private PrescriptionResponse mapToResponse(Prescription prescription) {
        return PrescriptionResponse.builder()
                .prescriptionId(prescription.getPrescriptionId())
                .medicalRecordId(prescription.getMedicalRecord().getRecordId())
                .notes(prescription.getNotes())
                .createdAt(prescription.getCreatedAt())
                .details(prescription.getDetails().stream()
                        .map(this::mapToDetailResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    private PrescriptionDetailResponse mapToDetailResponse(PrescriptionDetail detail) {
        return PrescriptionDetailResponse.builder()
                .id(detail.getId())
                .medicineName(detail.getMedicineName())
                .dosage(detail.getDosage())
                .quantity(detail.getQuantity())
                .build();
    }
}
