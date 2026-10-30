package g2.be.dto.response;

import lombok.*;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailableSlotResponse {
    private Long slotId;
    private String shiftName;
    private LocalTime startTime;
    private LocalTime endTime;
    private int maxPatients;
    private boolean isFull;
    private long currentCount;
}
