package g2.be.dto.request;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateWorkingTimeSlotRequest {

    @NotBlank(message = "Shift name is required")
    @Schema(description = "Shift name", example = "Ca Sáng")
    private String shiftName;

    @NotNull(message = "Start time is required")
    @JsonFormat(pattern = "HH:mm:ss")
    @Schema(description = "Start time in HH:mm:ss format", example = "07:00:00", type = "string")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    @JsonFormat(pattern = "HH:mm:ss")
    @Schema(description = "End time in HH:mm:ss format", example = "12:00:00", type = "string")
    private LocalTime endTime;

    @NotNull(message = "Max patients is required")
    @Min(value = 1, message = "Max patients must be at least 1")
    @Max(value = 10, message = "Max patients cannot exceed 10")
    @Schema(description = "Maximum number of patients for this slot", example = "8")
    private Integer maxPatients;

    @Schema(description = "Description of the shift", example = "Morning shift")
    private String description;

    @Schema(description = "Is active status", example = "true")
    private Boolean isActive;
}
