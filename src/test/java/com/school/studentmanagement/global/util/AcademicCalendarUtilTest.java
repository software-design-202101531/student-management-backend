package com.school.studentmanagement.global.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class AcademicCalendarUtilTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    // 주어진 날짜에 고정된 시계를 가진 유틸 생성
    private AcademicCalendarUtil at(LocalDate date) {
        Clock clock = Clock.fixed(date.atStartOfDay(SEOUL).toInstant(), SEOUL);
        return new AcademicCalendarUtil(clock);
    }

    @Test
    @DisplayName("학년도: 1~2월은 직전 학년도, 3월부터 당해 학년도")
    void currentAcademicYear() {
        assertThat(at(LocalDate.of(2026, 1, 15)).getCurrentAcademicYear()).isEqualTo(2025);
        assertThat(at(LocalDate.of(2026, 2, 28)).getCurrentAcademicYear()).isEqualTo(2025);
        assertThat(at(LocalDate.of(2026, 3, 1)).getCurrentAcademicYear()).isEqualTo(2026);
        assertThat(at(LocalDate.of(2026, 12, 31)).getCurrentAcademicYear()).isEqualTo(2026);
    }

    @Test
    @DisplayName("학기: 3~8월 1학기, 9월~익년 2월 2학기")
    void currentSemester() {
        assertThat(at(LocalDate.of(2026, 3, 1)).getCurrentSemester()).isEqualTo(1);
        assertThat(at(LocalDate.of(2026, 8, 31)).getCurrentSemester()).isEqualTo(1);
        assertThat(at(LocalDate.of(2026, 9, 1)).getCurrentSemester()).isEqualTo(2);
        assertThat(at(LocalDate.of(2026, 1, 31)).getCurrentSemester()).isEqualTo(2);
        assertThat(at(LocalDate.of(2026, 2, 28)).getCurrentSemester()).isEqualTo(2);
    }

    @Test
    @DisplayName("수정가능: 대상 학년도 다음 해 3/1 전까지 true (경계 포함 검증)")
    void isModifiable() {
        AcademicCalendarUtil mid2026 = at(LocalDate.of(2026, 6, 1));
        assertThat(mid2026.isModifiable(2026)).isTrue();   // 2027-03-01 전
        assertThat(mid2026.isModifiable(2025)).isFalse();  // 2026-03-01 후

        assertThat(at(LocalDate.of(2027, 2, 28)).isModifiable(2026)).isTrue();
        assertThat(at(LocalDate.of(2027, 3, 1)).isModifiable(2026)).isFalse();
    }
}
