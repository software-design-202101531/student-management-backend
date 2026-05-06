package com.school.studentmanagement.student.dto;

import com.school.studentmanagement.global.enums.ExamType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class StudentMyGradeResponse {

    private Integer academicYear;
    private Integer semester;
    private Integer totalScore;
    private Double averageScore;
    private List<ExamGradeDto> examGrades;

    @Getter
    @Builder
    public static class ExamGradeDto {
        private ExamType examType;
        private List<SubjectScoreDto> subjects;
    }

    @Getter
    @Builder
    public static class SubjectScoreDto {
        private String subjectName;
        private Integer rawScore;
    }
}
