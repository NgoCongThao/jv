package ngo.cong.thao.s2o_pro.common.exception;

import ngo.cong.thao.s2o_pro.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Bắt lỗi hệ thống chung
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        // Trong thực tế sẽ log lỗi ra file ở đây
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Lỗi máy chủ: " + ex.getMessage()));
    }

    // Bắt lỗi Validation (khi dùng @Valid ở Controller)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        FieldError firstError = ex.getBindingResult().getFieldErrors().get(0);
        String errorMessage = firstError.getField() + " " + firstError.getDefaultMessage();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, "Dữ liệu không hợp lệ: " + errorMessage));
    }
    // Bắt các lỗi do logic nghiệp vụ tự ném ra (ví dụ: Sai ID bàn, Chuyển sai trạng thái)
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiResponse<Void>> handleBusinessLogicException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST) // Ép về mã 400 Bad Request
                .body(ApiResponse.error(400, ex.getMessage()));
    }
    // Sau này chúng ta sẽ thêm các Custom Exception (như NotFoundException) vào đây
}