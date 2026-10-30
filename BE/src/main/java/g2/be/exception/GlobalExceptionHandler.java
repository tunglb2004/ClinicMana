package g2.be.exception;

import g2.be.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
        ApiResponse<Void> apiResponse = ApiResponse.error(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage());
        apiResponse.setError(ErrorCode.UNCATEGORIZED_EXCEPTION.name());
        // Add debug message for development (optional, remove in production)
        if (exception.getMessage() != null) {
            apiResponse.setMessage(exception.getMessage());
        }
        return ResponseEntity.badRequest().body(apiResponse);
    }

    @ExceptionHandler(value = org.springframework.dao.InvalidDataAccessApiUsageException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidDataAccessApiUsageException(
            org.springframework.dao.InvalidDataAccessApiUsageException exception) {
        ApiResponse<Void> apiResponse = ApiResponse.error("Yêu cầu dữ liệu không hợp lệ (Lỗi sort hoặc query).");
        apiResponse.setError("INVALID_DATA_ACCESS");
        if (exception.getMessage() != null) {
            apiResponse.setMessage("Lỗi truy vấn: " + exception.getMessage());
        }
        return ResponseEntity.badRequest().body(apiResponse);
    }

    @ExceptionHandler(value = org.springframework.security.authentication.BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(
            org.springframework.security.authentication.BadCredentialsException exception) {
        ErrorCode errorCode = ErrorCode.LOGIN_INVALID_CREDENTIALS;
        ApiResponse<Void> apiResponse = ApiResponse.error(errorCode.getMessage());
        apiResponse.setError(errorCode.name());
        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(value = AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        ApiResponse<Void> apiResponse = ApiResponse.error(errorCode.getMessage());
        apiResponse.setError(errorCode.name());
        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(apiResponse);
    }

    @ExceptionHandler(value = org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            org.springframework.security.access.AccessDeniedException exception) {
        // Using FORBIDDEN status but standard error class structure
        ApiResponse<Void> apiResponse = ApiResponse.error("Bạn không có quyền truy cập.", "ACCESS_DENIED");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(apiResponse);
    }

    @ExceptionHandler(value = org.springframework.security.authentication.LockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleLockedException(
            org.springframework.security.authentication.LockedException exception) {
        ErrorCode errorCode = ErrorCode.LOGIN_ACCOUNT_LOCKED;
        ApiResponse<Void> apiResponse = ApiResponse.error(errorCode.getMessage());
        apiResponse.setError(errorCode.name());
        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(value = org.springframework.security.authentication.DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisabledException(
            org.springframework.security.authentication.DisabledException exception) {
        ErrorCode errorCode = ErrorCode.LOGIN_ACCOUNT_LOCKED;
        ApiResponse<Void> apiResponse = ApiResponse.error("Tài khoản đã bị vô hiệu hóa.", "ACCOUNT_DISABLED");
        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .collect(Collectors.toMap(
                        error -> ((FieldError) error).getField(),
                        error -> error.getDefaultMessage(),
                        (existing, replacement) -> existing));

        ApiResponse<Map<String, String>> response = ApiResponse.error(
                "Dữ liệu không hợp lệ",
                "VALIDATION_ERROR");
        response.setData(errors);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
}
