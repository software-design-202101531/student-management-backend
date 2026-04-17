package com.school.studentmanagement.attendance.dto;

import com.school.studentmanagement.global.enums.AttendanceStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class AttendanceSaveRequest {
    private List<StudentAttendanceUpdateDto> attendanceData;

    @Getter
    @NoArgsConstructor
    public static class StudentAttendanceUpdateDto {
        private Long studentId;
        private AttendanceStatus status;
        private String reason;
    }
}
