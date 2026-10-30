package g2.be.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class CreateServiceOrderRequest {
    private String recordId;
    private List<String> serviceIds;
    private String indicationNote;
}
