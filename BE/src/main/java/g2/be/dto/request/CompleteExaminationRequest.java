package g2.be.dto.request;

import lombok.Data;

@Data
public class CompleteExaminationRequest {
    private String symptoms;
    private String diagnosis;
    private String doctorNotes;
}
