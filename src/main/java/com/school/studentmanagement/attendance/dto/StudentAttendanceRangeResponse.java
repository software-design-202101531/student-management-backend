package com.school.studentmanagement.attendance.dto;

import com.school.studentmanagement.global.enums.AttendanceStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/**
 * 교사가 한 학생의 기간(from~to) 출결을 조회할 때 반환되는 응답.
 * PRESENT(정상 출결)는 row 가 없으므로 {@link #entries} 에는 결석/지각/조퇴 등 특이사항만 포함된다.
 * 정상 출결 일수는 (영업일 - 휴일 - 특이사항)으로 클라이언트에서 계산하거나, 본 응답의 summary 를 활용한다.
 */
@Getter
@Builder
public class StudentAttendanceRangeResponse {

    private Long studentId;
    private String studentName;
    private LocalDate from;
    private LocalDate to;
    private Summary summary;
    private List<Entry> entries;
    private List<Holiday> holidays;

    @Getter
    @Builder
    public static class Summary {
        private int absentDays;
        private int lateDays;
        private int earlyLeaveDays;
    }

    @Getter
    @Builder
    public static class Entry {
        private LocalDate date;
        private AttendanceStatus status;
        private String reason; // 결석/지각/조퇴 사유 (nullable)
    }

    @Getter
    @Builder
    public static class Holiday {
        private LocalDate date;
        private String name;
    }
}
