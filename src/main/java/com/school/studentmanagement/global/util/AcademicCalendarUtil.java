package com.school.studentmanagement.global.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class AcademicCalendarUtil {

    // 테스트 가능성을 위해 Clock 주입 (운영에서는 Asia/Seoul 시계 빈 사용)
    private final Clock clock;

    // 서버의 현재 날짜를 기준으로 학년도를 계산
    public int getCurrentAcademicYear() {
        LocalDate now = LocalDate.now(clock);
        int currentYear = now.getYear();
        int currentMonth = now.getMonthValue();

        // 1, 2월까지는 아직 직전 학년도로 취급
        if (currentMonth <= 2) {
            return currentYear - 1;
        }
        return currentYear;
    }

    // 서버의 현재 날짜를 기준으로 학기를 계산
    public int getCurrentSemester() {
        int currentMonth = LocalDate.now(clock).getMonthValue();

        // 3월 ~ 8월까지는 1학기
        if (currentMonth >= 3 && currentMonth <= 8) {
            return 1;
        }
        // 9월부터 다음 해 2월까지는 2학기
        return 2;
    }

    // 수정 가능 상태 확인 — 대상 학년도 다음 해 3월 1일 전까지 수정 가능
    public boolean isModifiable(int targetYear) {
        LocalDate now = LocalDate.now(clock);
        LocalDate deadline = LocalDate.of(targetYear + 1, 3, 1);
        return now.isBefore(deadline);
    }
}
