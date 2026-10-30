package g2.be.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import g2.be.util.ClinicTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    private boolean success;
    private String message;
    private T data;
    private String error;
    
    @Builder.Default
    private LocalDateTime timestamp = ClinicTime.now();

    // Success response with data
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(ClinicTime.now())
                .build();
    }

    // Success response without data
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .timestamp(ClinicTime.now())
                .build();
    }

    // Error response
    public static <T> ApiResponse<T> error(String message, String error) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .error(error)
                .timestamp(ClinicTime.now())
                .build();
    }

    // Error response without error code
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(ClinicTime.now())
                .build();
    }
}
