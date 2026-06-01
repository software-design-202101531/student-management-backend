package com.school.studentmanagement.global.validation;

import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageFileValidatorTest {

    private final ImageFileValidator validator = new ImageFileValidator();

    @Test
    @DisplayName("실패: 빈 파일 → EMPTY_FILE")
    void emptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMPTY_FILE);
    }

    @Test
    @DisplayName("실패: 5MB 초과 → FILE_TOO_LARGE")
    void tooLarge() {
        byte[] content = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", content);

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_TOO_LARGE);
    }

    @Test
    @DisplayName("실패: 허용되지 않은 확장자(.gif) → UNSUPPORTED_IMAGE_TYPE")
    void badExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "a.gif", "image/jpeg", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNSUPPORTED_IMAGE_TYPE);
    }

    @Test
    @DisplayName("실패: 확장자는 맞지만 content-type 불일치 → UNSUPPORTED_IMAGE_TYPE")
    void badContentType() {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "text/plain", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNSUPPORTED_IMAGE_TYPE);
    }

    @Test
    @DisplayName("성공: jpg/png/webp 통과")
    void validImages() {
        assertThatCode(() -> validator.validate(
                new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[]{1}))).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(
                new MockMultipartFile("file", "a.png", "image/png", new byte[]{1}))).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(
                new MockMultipartFile("file", "a.webp", "image/webp", new byte[]{1}))).doesNotThrowAnyException();
    }
}
