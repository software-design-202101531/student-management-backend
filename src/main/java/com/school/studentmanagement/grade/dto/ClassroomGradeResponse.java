package com.school.studentmanagement.grade.dto;

import com.school.studentmanagement.global.enums.ExamType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ClassroomGradeResponse {

    private Integer academicYear;
    private Integer semester;
    private ExamType examType;
    private List<StudentAllGradesDto> students;

    @Getter
    @Builder
    public static class StudentAllGradesDto {
        private Long studentId;
        private String studentName;
        private Integer studentNum;
        private Integer totalScore;     // 학기 누적 총점 (중간 + 기말 합산)
        private Double averageScore;    // 학기 누적 평균
        private List<SubjectScoreDto> subjectScores;  // 해당 시험의 과목별 성적
    }

    @Getter
    @Builder
    public static class SubjectScoreDto {
        private Long gradeId;
        private String subjectName;
        private Integer rawScore;
    }
}
