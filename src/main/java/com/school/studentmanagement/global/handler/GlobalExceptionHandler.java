package com.school.studentmanagement.global.handler;

import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.exception.ErrorCode;
import com.school.studentmanagement.global.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        ErrorCode code = e.getErrorCode();
        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.fail(code.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(ErrorCode.INVALID_INPUT.getCode(), message));
    }

    /**
     * @RequestParam / @PathVariable 등에 적용된 jakarta.validation 제약 위반.
     * 컨트롤러에 @Validated 가 있을 때 발생하며, 미적용 시엔 발생하지 않음(미적용이 일반 정책).
     * 발생 시 500 대신 400 으로 매핑해 클라이언트 입력 오류임을 정확히 알린다.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(v -> {
                    String path = v.getPropertyPath().toString();
                    // ex) "saveDailyAttendance.date" → "date" 만 노출하면 클라이언트 친화적
                    int dot = path.lastIndexOf('.');
                    String field = dot >= 0 ? path.substring(dot + 1) : path;
                    return field + ": " + v.getMessage();
                })
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(ErrorCode.INVALID_INPUT.getCode(), message));
    }

    /** 필수 @RequestParam 누락 — Spring 기본은 400 이지만 응답 포맷을 ApiResponse 로 통일. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException e) {
        String message = e.getParameterName() + " 파라미터는 필수입니다";
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(ErrorCode.INVALID_INPUT.getCode(), message));
    }

    /** 쿼리 파라미터·경로변수의 타입 변환 실패(예: ?page=abc, enum 미일치). 500 대신 400 으로. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String expected = e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "예상 타입";
        String message = e.getName() + " 파라미터의 형식이 올바르지 않습니다(" + expected + ")";
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(ErrorCode.INVALID_INPUT.getCode(), message));
    }

    // 멀티파트 업로드 용량 초과 — 컨테이너 단계에서 차단되므로 400(FILE_TOO_LARGE)로 매핑
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        ErrorCode code = ErrorCode.FILE_TOO_LARGE;
        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.fail(code.getCode(), code.getMessage()));
    }

    // 유니크/제약 위반 등 DB 무결성 예외를 500 대신 409로 매핑 (find-then-save 경합 등)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException e) {
        log.warn("Data integrity violation", e);
        ErrorCode code = ErrorCode.DATA_INTEGRITY_VIOLATION;
        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.fail(code.getCode(), code.getMessage()));
    }

    // 낙관적 락(@Version) 충돌 — 동시 수정 시 나중 커밋을 409로 거부해 갱신 손실을 방지
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLock(ObjectOptimisticLockingFailureException e) {
        log.warn("Optimistic lock conflict", e);
        ErrorCode code = ErrorCode.RECORD_CONFLICT;
        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.fail(code.getCode(), code.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("Unexpected server error", e);
        ErrorCode code = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity.internalServerError()
                .body(ApiResponse.fail(code.getCode(), code.getMessage()));
    }
}
