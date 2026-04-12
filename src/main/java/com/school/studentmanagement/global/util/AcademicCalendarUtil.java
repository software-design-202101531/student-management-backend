package com.school.studentmanagement.global.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class AcademicCalendarUtil {

    // 서버의 현재 날짜를 기준으로 진짜 학년도를 계산
    public int getCurrentAcademicYear() {
        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        int currentMonth = now.getMonthValue();

        // 1, 2월까지는 아직 학기로 취급
        if (currentMonth <= 2) {
            return currentYear - 1;
        }
        return currentYear;
    }

    // 서버의 현재 날짜를 기준으로 학기를 계산
    public int getCurrentSemester() {

        int currentMonth = LocalDate.now().getMonthValue();

        // 3월 ~ 8월까지는 1학기
        if (currentMonth >= 3 && currentMonth <= 8) {
            return 1;
        }
        // 9월부터 다음 해 2월까지는 2학기
        return 2;
    }
}
