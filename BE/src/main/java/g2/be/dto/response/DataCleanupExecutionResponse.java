package g2.be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataCleanupExecutionResponse {

    private DataCleanupReport before;

    private DataCleanupReport after;

    @Builder.Default
    private List<String> actions = new ArrayList<>();
}