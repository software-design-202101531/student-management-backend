package com.school.studentmanagement.student.dto;

import com.school.studentmanagement.global.enums.AttendanceStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class StudentMyAttendanceResponse {

    private Integer year;
    private Integer month;

    // 월 합계 (PRESENT가 아닌 row만 집계)
    private Integer absentDays;
    private Integer lateDays;
    private Integer earlyLeaveDays;

    // 특이사항이 있는 일자만 (PRESENT는 row가 없으므로 자연히 제외)
    private List<AttendanceEntry> entries;

    // 휴일 정보 (출결 의무 없는 날)
    private List<HolidayDto> holidays;

    @Getter
    @Builder
    public static class AttendanceEntry {
        private LocalDate date;
        private AttendanceStatus status;
        private String reason;
    }

    @Getter
    @Builder
    public static class HolidayDto {
        private LocalDate date;
        private String name;
    }
}
