package g2.be.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceRoomResponse {

    private String roomId;
    private String roomName;
    private List<ServiceInfo> services;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ServiceInfo {
        private String serviceId;
        private String serviceName;
    }
}
