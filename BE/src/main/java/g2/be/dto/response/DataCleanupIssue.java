package g2.be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataCleanupIssue {

    private String category;
    private String entityType;
    private String entityId;
    private String currentValue;
    private String expectedValue;
    private String action;
}