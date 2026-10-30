package g2.be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomTransferOptionResponse {
    private String roomId;
    private String roomName;
    private String departmentName;
    private String location;
    private Integer currentQueueLength;
    private Integer baseAvgTime;
    private String doctorId;
    private String doctorName;
    private Boolean currentRoom;
    private String serviceId;
    private String serviceName;
}
