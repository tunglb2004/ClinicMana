package g2.be.controller;

import g2.be.dto.response.CombinedXmlResponse;
import g2.be.entity.ReceivedClaim;
import g2.be.repository.ReceivedClaimRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.UUID;

@RestController
@RequestMapping("/api/mock-bhxh")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Mock BHXH Server", description = "Mock API nhận hồ sơ từ phòng khám")
public class MockBhxhServerController {

    private final ReceivedClaimRepository receivedClaimRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/receive")
    @Operation(summary = "Nhận hồ sơ XML tổng hợp", description = "Endpoint nhận dữ liệu từ phòng khám")
    public ResponseEntity<?> receiveClaim(
            @RequestBody CombinedXmlResponse payload,
            @RequestParam(defaultValue = "false") boolean simulateError) {

        log.info("Mock BHXH Server received claim for patient: {}", payload.getXml1().getHoTen());

        if (simulateError) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Kết nối lỗi - Giả lập sự cố rớt mạng!");
        }

        try {
            ReceivedClaim claim = new ReceivedClaim();
            claim.setSubmissionCode(UUID.randomUUID().toString());
            claim.setCccdNumber(payload.getXml1().getSoCccd());
            claim.setInsuranceCode(payload.getXml1().getMaTheBhyt());
            claim.setPatientName(payload.getXml1().getHoTen());
            claim.setMaDkbd(payload.getXml1().getMaDkbd());
            claim.setTotalAmount(payload.getXml1().getTTongchiBv());
            claim.setInsuranceClaimAmount(payload.getXml1().getTTongchiBh());
            claim.setPatientPaid(payload.getXml1().getTBntt());
            claim.setXmlContent(objectMapper.writeValueAsString(payload));
            claim.setStatus("RECEIVED");

            receivedClaimRepository.save(claim);

            return ResponseEntity.ok("OK - Đã ghi nhận hồ sơ");
        } catch (Exception e) {
            log.error("Lỗi khi xử lý hồ sơ: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi xử lý nội bộ");
        }
    }
}
