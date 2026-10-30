package g2.be.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransferAppointmentRoomRequest {
    private String roomId;
    private String reason;
}
