package com.school.studentmanagement.grade.dto;

import com.school.studentmanagement.global.enums.ExamAttendanceStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GradeUpdateRequest {

    // PRESENT면 필수, ABSENT/CHEATED면 무시
    @Min(0)
    private Integer rawScore;

    // 미지정 시 PRESENT
    private ExamAttendanceStatus attendanceStatus;

    @Size(max = 200)
    private String reason;   // 옵셔널 — 변경 이력에 함께 기록

    @Builder
    public GradeUpdateRequest(Integer rawScore, ExamAttendanceStatus attendanceStatus, String reason) {
        this.rawScore = rawScore;
        this.attendanceStatus = attendanceStatus;
        this.reason = reason;
    }
}
