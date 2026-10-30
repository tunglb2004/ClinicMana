package g2.be.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import g2.be.entity.Appointment;
import g2.be.entity.MedicalRecord;
import g2.be.entity.Patient;
import g2.be.entity.Prescription;
import g2.be.entity.PrescriptionDetail;
import g2.be.entity.Room;
import g2.be.entity.ServiceOrder;
import g2.be.entity.User;
import g2.be.util.ServiceOrderResultFiles;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class PdfExportService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] generateServiceOrderPdf(ServiceOrder order) {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Setup basic fonts (using standard fonts, might not support full Vietnamese tonal marks perfectly without TTF)
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.BLACK);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.BLACK);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.BLACK);

            // Title
            Paragraph title = new Paragraph("Medical Examination Result", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(Chunk.NEWLINE);

            // Patient Info Table
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingBefore(10f);

            addCellToTable(infoTable, "Order ID:", headerFont);
            addCellToTable(infoTable, order.getOrderId(), bodyFont);

            addCellToTable(infoTable, "Patient Name:", headerFont);
            addCellToTable(infoTable, order.getMedicalRecord().getPatient().getUser().getFullName(), bodyFont);

            addCellToTable(infoTable, "Service:", headerFont);
            addCellToTable(infoTable, order.getService().getServiceName(), bodyFont);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
            addCellToTable(infoTable, "Created Date:", headerFont);
            addCellToTable(infoTable, order.getCreatedAt() != null ? order.getCreatedAt().format(formatter) : "N/A", bodyFont);

            addCellToTable(infoTable, "Status:", headerFont);
            addCellToTable(infoTable, order.getStatus(), bodyFont);

            document.add(infoTable);
            document.add(Chunk.NEWLINE);

            // Result Note Section
            Paragraph resultHeader = new Paragraph("Examination Result Note:", headerFont);
            document.add(resultHeader);
            
            Paragraph resultNote = new Paragraph(order.getResultNote() != null ? order.getResultNote() : "No result written yet.", bodyFont);
            document.add(resultNote);

            document.add(Chunk.NEWLINE);

            // Footer
            Paragraph footer = new Paragraph("System Generated Document", bodyFont);
            footer.setAlignment(Element.ALIGN_RIGHT);
            document.add(footer);

            document.close();
        } catch (DocumentException e) {
            e.printStackTrace();
        }

        return out.toByteArray();
    }

    public byte[] generateMedicalRecordPdf(
            MedicalRecord record,
            List<ServiceOrder> serviceOrders,
            Prescription prescription) {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, BaseColor.BLACK);
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.DARK_GRAY);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, BaseColor.BLACK);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.BLACK);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLACK);

            addMedicalRecordHeader(document, record, titleFont, subtitleFont);
            addPatientSection(document, record, sectionFont, headerFont, bodyFont);
            addClinicalSection(document, record, sectionFont, headerFont, bodyFont);
            addParaclinicalSection(document, serviceOrders, sectionFont, headerFont, bodyFont);
            addPrescriptionSection(document, prescription, sectionFont, headerFont, bodyFont);
            addSignatureSection(document, record, headerFont, bodyFont);

            document.close();
        } catch (DocumentException ex) {
            throw new IllegalStateException("Failed to generate medical record PDF", ex);
        }

        return out.toByteArray();
    }

    private void addMedicalRecordHeader(Document document, MedicalRecord record, Font titleFont, Font subtitleFont)
            throws DocumentException {
        Paragraph clinicName = new Paragraph("G2 CLINIC", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.DARK_GRAY));
        clinicName.setAlignment(Element.ALIGN_LEFT);
        document.add(clinicName);

        Paragraph title = new Paragraph("MEDICAL RECORD", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingBefore(6f);
        document.add(title);

        Paragraph subtitle = new Paragraph(
                "Generated at " + formatDateTime(LocalDateTime.now()) + " | Record " + text(record.getRecordId()),
                subtitleFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(12f);
        document.add(subtitle);
    }

    private void addPatientSection(Document document, MedicalRecord record, Font sectionFont, Font headerFont, Font bodyFont)
            throws DocumentException {
        Appointment appointment = record.getAppointment();
        Patient patient = record.getPatient();
        User patientUser = patient != null ? patient.getUser() : null;
        Room room = appointment != null ? appointment.getRoom() : null;

        addSectionTitle(document, "Patient and visit information", sectionFont);
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.4f, 2.4f, 1.4f, 2.4f});

        addInfoRow(table, "Record ID", record.getRecordId(), "Appointment ID", appointment != null ? appointment.getAppointmentId() : null, headerFont, bodyFont);
        addInfoRow(table, "Patient ID", patient != null ? patient.getPatientId() : null, "Patient name", patientUser != null ? patientUser.getFullName() : null, headerFont, bodyFont);
        addInfoRow(table, "Date of birth", patient != null ? formatDate(patient.getDateOfBirth()) : null, "Gender", patientUser != null && patientUser.getGender() != null ? patientUser.getGender().name() : null, headerFont, bodyFont);
        addInfoRow(table, "Phone", patientUser != null ? patientUser.getPhoneNumber() : null, "Health insurance", patient != null ? patient.getHealthInsuranceNumber() : null, headerFont, bodyFont);
        addInfoRow(table, "Visit date", appointment != null ? formatDate(appointment.getAppointmentDate()) : formatDateTime(record.getCreatedAt()), "Visit time", appointment != null ? formatTime(appointment.getAppointmentTime()) : null, headerFont, bodyFont);
        addInfoRow(table, "Doctor", doctorName(record), "Department", departmentName(record), headerFont, bodyFont);
        addInfoRow(table, "Room", room != null ? room.getRoomName() : null, "Status", record.getStatus(), headerFont, bodyFont);
        document.add(table);
        document.add(Chunk.NEWLINE);
    }

    private void addClinicalSection(Document document, MedicalRecord record, Font sectionFont, Font headerFont, Font bodyFont)
            throws DocumentException {
        Map<String, Object> symptoms = parseSymptoms(record.getSymptoms());

        addSectionTitle(document, "Clinical examination", sectionFont);
        PdfPTable vitals = new PdfPTable(4);
        vitals.setWidthPercentage(100);
        vitals.setWidths(new float[]{1.5f, 2f, 1.5f, 2f});
        addInfoRow(vitals, "Pulse", symptom(symptoms, "mach", "pulse"), "Blood pressure", symptom(symptoms, "huyetAp", "bloodPressure"), headerFont, bodyFont);
        addInfoRow(vitals, "Temperature", symptom(symptoms, "nhietDo", "temperature"), "Heart rate", symptom(symptoms, "nhipTim", "heartRate"), headerFont, bodyFont);
        addInfoRow(vitals, "Weight", symptom(symptoms, "canNang", "weight"), "Height", symptom(symptoms, "chieuCao", "height"), headerFont, bodyFont);
        addInfoRow(vitals, "Defecation", symptom(symptoms, "daiTien"), "Urination", symptom(symptoms, "tieuTien"), headerFont, bodyFont);
        document.add(vitals);

        addTextBlock(document, "Symptom description", symptom(symptoms, "moTa", "description", "physicalExaminationNotes", "raw"), headerFont, bodyFont);
        addTextBlock(document, "Diagnosis", record.getDiagnosis(), headerFont, bodyFont);
        addTextBlock(document, "Doctor notes", record.getDoctorNotes(), headerFont, bodyFont);
    }

    private void addParaclinicalSection(Document document, List<ServiceOrder> serviceOrders, Font sectionFont, Font headerFont, Font bodyFont)
            throws DocumentException {
        addSectionTitle(document, "Paraclinical results", sectionFont);
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{0.6f, 2.2f, 1.2f, 2.8f, 2.2f});
        addTableHeader(table, List.of("No.", "Service", "Status", "Result / note", "Attached files"), headerFont);

        List<ServiceOrder> activeOrders = serviceOrders == null
                ? List.of()
                : serviceOrders.stream()
                        .filter(order -> order != null && !"CANCELLED".equalsIgnoreCase(order.getStatus()))
                        .toList();

        if (activeOrders.isEmpty()) {
            PdfPCell emptyCell = new PdfPCell(new Phrase("No paraclinical results.", bodyFont));
            emptyCell.setColspan(5);
            emptyCell.setPadding(6f);
            table.addCell(emptyCell);
        } else {
            for (int index = 0; index < activeOrders.size(); index++) {
                ServiceOrder order = activeOrders.get(index);
                table.addCell(bodyCell(String.valueOf(index + 1), bodyFont));
                table.addCell(bodyCell(order.getService() != null ? order.getService().getServiceName() : "", bodyFont));
                table.addCell(bodyCell(order.getStatus(), bodyFont));
                table.addCell(bodyCell(order.getResultNote(), bodyFont));
                table.addCell(bodyCell(String.join("\n", ServiceOrderResultFiles.parse(order.getResultFileUrl())), bodyFont));
            }
        }

        document.add(table);
        document.add(Chunk.NEWLINE);
    }

    private void addPrescriptionSection(Document document, Prescription prescription, Font sectionFont, Font headerFont, Font bodyFont)
            throws DocumentException {
        addSectionTitle(document, "Prescription", sectionFont);

        if (prescription == null || prescription.getDetails() == null || prescription.getDetails().isEmpty()) {
            addTextBlock(document, "Prescription details", "No prescription.", headerFont, bodyFont);
            return;
        }

        if (prescription.getNotes() != null && !prescription.getNotes().isBlank()) {
            addTextBlock(document, "Prescription notes", prescription.getNotes(), headerFont, bodyFont);
        }

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{0.7f, 3.2f, 3f, 1.4f});
        addTableHeader(table, List.of("No.", "Medicine", "Dosage", "Quantity"), headerFont);

        List<PrescriptionDetail> details = prescription.getDetails();
        for (int index = 0; index < details.size(); index++) {
            PrescriptionDetail detail = details.get(index);
            table.addCell(bodyCell(String.valueOf(index + 1), bodyFont));
            table.addCell(bodyCell(detail.getMedicineName(), bodyFont));
            table.addCell(bodyCell(detail.getDosage(), bodyFont));
            table.addCell(bodyCell(detail.getQuantity(), bodyFont));
        }

        document.add(table);
        document.add(Chunk.NEWLINE);
    }

    private void addSignatureSection(Document document, MedicalRecord record, Font headerFont, Font bodyFont)
            throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(12f);

        PdfPCell doctorCell = new PdfPCell();
        doctorCell.setBorder(Rectangle.NO_BORDER);
        doctorCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        doctorCell.addElement(centered("Doctor", headerFont));
        doctorCell.addElement(new Paragraph("\n\n\n"));
        doctorCell.addElement(centered(doctorName(record), bodyFont));
        table.addCell(doctorCell);

        PdfPCell patientCell = new PdfPCell();
        patientCell.setBorder(Rectangle.NO_BORDER);
        patientCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        patientCell.addElement(centered("Patient / guardian", headerFont));
        patientCell.addElement(new Paragraph("\n\n\n"));
        patientCell.addElement(centered("(Signature and full name)", bodyFont));
        table.addCell(patientCell);

        document.add(table);
    }

    private void addCellToTable(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(5f);
        table.addCell(cell);
    }

    private void addSectionTitle(Document document, String title, Font font) throws DocumentException {
        Paragraph paragraph = new Paragraph(title, font);
        paragraph.setSpacingBefore(6f);
        paragraph.setSpacingAfter(6f);
        document.add(paragraph);
    }

    private void addInfoRow(PdfPTable table, String label1, String value1, String label2, String value2, Font headerFont, Font bodyFont) {
        table.addCell(labelCell(label1, headerFont));
        table.addCell(bodyCell(value1, bodyFont));
        table.addCell(labelCell(label2, headerFont));
        table.addCell(bodyCell(value2, bodyFont));
    }

    private void addTextBlock(Document document, String label, String value, Font headerFont, Font bodyFont) throws DocumentException {
        Paragraph header = new Paragraph(label + ":", headerFont);
        header.setSpacingBefore(8f);
        document.add(header);

        Paragraph body = new Paragraph(blankToFallback(value), bodyFont);
        body.setSpacingAfter(4f);
        document.add(body);
    }

    private void addTableHeader(PdfPTable table, List<String> headers, Font headerFont) {
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setBackgroundColor(new BaseColor(238, 242, 247));
            cell.setPadding(6f);
            table.addCell(cell);
        }
    }

    private PdfPCell labelCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(blankToFallback(text), font));
        cell.setBackgroundColor(new BaseColor(248, 250, 252));
        cell.setPadding(5f);
        return cell;
    }

    private PdfPCell bodyCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(blankToFallback(text), font));
        cell.setPadding(5f);
        return cell;
    }

    private Paragraph centered(String text, Font font) {
        Paragraph paragraph = new Paragraph(blankToFallback(text), font);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        return paragraph;
    }

    private Map<String, Object> parseSymptoms(String rawSymptoms) {
        if (rawSymptoms == null || rawSymptoms.isBlank()) {
            return Map.of();
        }

        try {
            return OBJECT_MAPPER.readValue(rawSymptoms, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ignored) {
            return Map.of("raw", rawSymptoms);
        }
    }

    private String symptom(Map<String, Object> symptoms, String... keys) {
        for (String key : keys) {
            Object value = symptoms.get(key);
            if (value != null && !String.valueOf(value).isBlank()) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    private String doctorName(MedicalRecord record) {
        return record.getDoctor() != null && record.getDoctor().getUser() != null
                ? record.getDoctor().getUser().getFullName()
                : null;
    }

    private String departmentName(MedicalRecord record) {
        return record.getDoctor() != null && record.getDoctor().getDepartment() != null
                ? record.getDoctor().getDepartment().getDeptName()
                : null;
    }

    private String blankToFallback(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }

    private String text(String value) {
        return value == null ? "" : value;
    }

    private String formatDate(LocalDate value) {
        return value == null ? null : value.format(DATE_FORMATTER);
    }

    private String formatTime(LocalTime value) {
        return value == null ? null : value.format(TIME_FORMATTER);
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.format(DATE_TIME_FORMATTER);
    }
}
