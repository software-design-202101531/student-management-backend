package com.school.studentmanagement.global.handler;

import com.school.studentmanagement.global.response.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.lang.reflect.Method;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 새로 추가된 핸들러(ConstraintViolation / MissingServletRequestParameter / MethodArgumentTypeMismatch)가
 * 500 이 아닌 400 + INVALID_INPUT 포맷으로 매핑되는지 검증.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("ConstraintViolation → 400 INVALID_INPUT, 짧은 필드명 포함")
    void constraintViolation() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Set<ConstraintViolation<TestBean>> violations =
                validator.validate(new TestBean("", -1));

        ConstraintViolationException ex = new ConstraintViolationException(violations);

        ResponseEntity<ApiResponse<Void>> response = handler.handleConstraintViolation(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getError().getCode()).isEqualTo("INVALID_INPUT");
        // 짧은 필드명 추출(클래스명/메서드명 접두 제거)
        assertThat(response.getBody().getError().getMessage())
                .contains("name").contains("page");
        assertThat(response.getBody().getError().getMessage())
                .doesNotContain("TestBean.");
    }

    @Test
    @DisplayName("MissingServletRequestParameter → 400 INVALID_INPUT")
    void missingParam() {
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("year", "Integer");

        ResponseEntity<ApiResponse<Void>> response = handler.handleMissingParam(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getError().getCode()).isEqualTo("INVALID_INPUT");
        assertThat(response.getBody().getError().getMessage()).contains("year").contains("필수");
    }

    @Test
    @DisplayName("MethodArgumentTypeMismatch → 400 INVALID_INPUT (예상 타입 표시)")
    void typeMismatch() throws NoSuchMethodException {
        Method m = SampleController.class.getDeclaredMethod("get", Integer.class);
        MethodParameter param = new MethodParameter(m, 0);

        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "abc", Integer.class, "page", param, new IllegalArgumentException("nope"));

        ResponseEntity<ApiResponse<Void>> response = handler.handleTypeMismatch(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getError().getCode()).isEqualTo("INVALID_INPUT");
        assertThat(response.getBody().getError().getMessage())
                .contains("page").contains("Integer");
    }

    // ─── 테스트 픽스처 ────────────────────────────────────────────────────
    private record TestBean(@NotBlank String name, @Min(0) int page) {}

    static class SampleController {
        void get(Integer page) {}
    }
}
