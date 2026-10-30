package g2.be.dto.response;

import java.time.LocalDateTime;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkingTimeSlotResponse {

    private Long slotId;
    
    private String shiftName;
    
    private LocalTime startTime;
    
    private LocalTime endTime;

    private Integer maxPatients;
    
    private String description;
    
    private Boolean isActive;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
