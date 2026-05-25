package com.school.studentmanagement.grade.dto;

import com.school.studentmanagement.global.enums.ExamAttendanceStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GradeSaveRequest {

    @NotNull
    private Long examId;

    @NotEmpty
    private List<StudentScoreDto> scores;

    @Builder
    public GradeSaveRequest(Long examId, List<StudentScoreDto> scores) {
        this.examId = examId;
        this.scores = scores;
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class StudentScoreDto {

        @NotNull
        private Long studentId;

        // PRESENT면 필수, ABSENT/CHEATED면 무시 (서비스에서 정규화)
        @Min(0)
        private Integer rawScore;

        // 미지정 시 PRESENT
        private ExamAttendanceStatus attendanceStatus;

        @Builder
        public StudentScoreDto(Long studentId, Integer rawScore, ExamAttendanceStatus attendanceStatus) {
            this.studentId = studentId;
            this.rawScore = rawScore;
            this.attendanceStatus = attendanceStatus;
        }
    }
}
