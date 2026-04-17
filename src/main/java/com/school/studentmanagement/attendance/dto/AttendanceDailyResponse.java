package com.school.studentmanagement.attendance.dto;

import com.school.studentmanagement.global.enums.AttendanceStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;


@Getter
@Builder
public class AttendanceDailyResponse {
    // 날짜 선택 후 해당 반의 명단과 현재 저장된 출결 상태를 가져온다
    private LocalDate date;
    private boolean isHoliday;                      // 휴일 여부
    private String holidayName;                     // 휴일 이름
    private List<StudentAttendanceDto> students;    // 학생 전원 명단

    @Getter
    @Builder
    public static class StudentAttendanceDto {
        private Long studentId;
        private Integer studentNum;         // 출석번호
        private String studentName;         // 학생이름
        private AttendanceStatus status;    // 출결 상태
        private String reason;              // 특이사항
    }
}
