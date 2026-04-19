package com.school.studentmanagement.grade.dto;

import com.school.studentmanagement.global.enums.ExamType;
import jakarta.validation.constraints.Max;
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
    private Integer academicYear;

    @NotNull
    private Integer semester;

    @NotNull
    private ExamType examType;

    @NotEmpty
    private List<StudentScoreDto> scores;

    @Builder
    public GradeSaveRequest(Integer academicYear, Integer semester, ExamType examType, List<StudentScoreDto> scores) {
        this.academicYear = academicYear;
        this.semester = semester;
        this.examType = examType;
        this.scores = scores;
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class StudentScoreDto {

        @NotNull
        private Long studentId;

        @NotNull
        @Min(0) @Max(100)
        private Integer rawScore;

        @Builder
        public StudentScoreDto(Long studentId, Integer rawScore) {
            this.studentId = studentId;
            this.rawScore = rawScore;
        }
    }
}
