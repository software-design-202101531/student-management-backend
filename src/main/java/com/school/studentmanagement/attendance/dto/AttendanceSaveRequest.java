package com.school.studentmanagement.attendance.dto;

import com.school.studentmanagement.global.enums.AttendanceStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class AttendanceSaveRequest {

    @NotEmpty(message = "출결 데이터는 비어있을 수 없습니다")
    @Valid
    private List<StudentAttendanceUpdateDto> attendanceData;

    @Getter
    @NoArgsConstructor
    public static class StudentAttendanceUpdateDto {

        @NotNull(message = "학생 ID는 필수입니다")
        private Long studentId;

        @NotNull(message = "출결 상태는 필수입니다")
        private AttendanceStatus status;

        private String reason;
    }
}
