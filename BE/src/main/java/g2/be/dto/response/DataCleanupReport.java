package g2.be.dto.response;

import g2.be.util.ClinicTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataCleanupReport {

    @Builder.Default
    private Map<String, Integer> counts = new LinkedHashMap<>();

    @Builder.Default
    private List<DataCleanupIssue> issues = new ArrayList<>();

    @Builder.Default
    private List<String> notes = new ArrayList<>();

    @Builder.Default
    private LocalDateTime generatedAt = ClinicTime.now();
}
