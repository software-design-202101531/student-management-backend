package com.school.studentmanagement.global.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code logback-spring.xml} 의 마스킹 정규식이 실제 logback PatternLayout 으로
 * 컴파일 가능하고 의도대로 동작하는지 검증한다.
 *
 * <p>logback 의 {@code %replace} 체이닝은 패턴 문법 수준에서 오류가 나면 시작 단계에서
 * 조용히 무시될 수 있어, 패턴이 유효한지(layout.isStarted())와 실제 마스킹 결과를 함께 확인.</p>
 */
class LogMaskingTest {

    /** logback-spring.xml 의 MASKED_MSG 와 동일한 패턴 (테스트 단순화를 위해 색상/날짜 등 제거하고 메시지 부분만). */
    private static final String MASK_PATTERN =
            "%replace(" +
            "%replace(" +
            "%replace(" +
            "%replace(" +
            "%replace(" +
            "%replace(" +
            "%replace(%m){'(Bearer\\s+)[A-Za-z0-9._\\-]+', '$1***'}" +
            "){'(?i)(authorization\\s*[:=]\\s*)\\S+', '$1***'}" +
            "){'(?i)(set-cookie\\s*[:=]\\s*)\\S+', '$1***'}" +
            "){'(?i)(cookie\\s*[:=]\\s*)\\S+', '$1***'}" +
            "){'(?i)(\"password\"\\s*:\\s*\")[^\"]*', '$1***'}" +
            "){'(?i)(\"token\"\\s*:\\s*\")[^\"]*', '$1***'}" +
            "){'(?i)(\"secret\"\\s*:\\s*\")[^\"]*', '$1***'}";

    private PatternLayout layout;

    @BeforeEach
    void setUp() {
        LoggerContext context = new LoggerContext();
        layout = new PatternLayout();
        layout.setContext(context);
        layout.setPattern(MASK_PATTERN);
        layout.start();
    }

    private String format(String message) {
        LoggingEvent event = new LoggingEvent();
        event.setLevel(ch.qos.logback.classic.Level.toLevel(Level.INFO.toString()));
        event.setMessage(message);
        return layout.doLayout(event);
    }

    @Test
    @DisplayName("패턴이 logback 에서 유효하게 시작됨")
    void layoutStarts() {
        assertThat(layout.isStarted()).isTrue();
    }

    @Test
    @DisplayName("Bearer 토큰 → 'Bearer ***'")
    void masksBearerToken() {
        String masked = format("authHeader=Bearer eyJhbGciOi.JIUzI1NiJ9.abc-123_xyz rest");
        assertThat(masked).contains("Bearer ***");
        assertThat(masked).doesNotContain("eyJhbGciOi");
    }

    @Test
    @DisplayName("Authorization: 헤더 마스킹")
    void masksAuthorizationHeader() {
        String masked = format("got Authorization: Bearer something-secret tail");
        // 첫 번째 패턴이 Bearer를, 두 번째가 Authorization 뒤 비공백을 마스킹 — 어느 쪽이든 비밀이 안 보이면 성공.
        assertThat(masked).doesNotContain("something-secret");
    }

    @Test
    @DisplayName("Cookie / Set-Cookie 헤더 마스킹")
    void masksCookieHeaders() {
        String c = format("Cookie: refreshToken=abcdef123; Path=/");
        String sc = format("Set-Cookie: refreshToken=abcdef123; HttpOnly");
        assertThat(c).doesNotContain("abcdef123");
        assertThat(sc).doesNotContain("abcdef123");
    }

    @Test
    @DisplayName("JSON password/token/secret 값 마스킹")
    void masksJsonSensitiveFields() {
        String body = "request body: {\"loginId\":\"alice\",\"password\":\"super-secret\",\"token\":\"abc.def\",\"secret\":\"xyz\"}";
        String masked = format(body);
        assertThat(masked).doesNotContain("super-secret");
        assertThat(masked).doesNotContain("abc.def");
        assertThat(masked).doesNotContain("\"secret\":\"xyz\"");
        assertThat(masked).contains("\"password\":\"***\"");
        assertThat(masked).contains("\"loginId\":\"alice\""); // 비민감 필드는 그대로
    }

    @Test
    @DisplayName("민감 키워드가 없는 메시지는 그대로")
    void doesNotChangeOrdinaryMessage() {
        String message = "학생 ID 10 의 피드백 3건 조회 완료";
        assertThat(format(message)).contains(message);
    }
}
