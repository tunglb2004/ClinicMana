package g2.be.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataCleanupExecuteRequest {

    private Boolean confirmExecution;

    private String confirmationText;

    private Integer expectedIssueCount;
}