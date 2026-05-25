package com.school.studentmanagement.student.dto;

import com.school.studentmanagement.global.enums.ExamAttendanceStatus;
import com.school.studentmanagement.global.enums.ExamType;
import com.school.studentmanagement.global.enums.GradeLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class StudentMyGradeResponse {

    private Integer academicYear;
    private Integer semester;

    // 학기 누적 통계
    private Double totalScore;
    private Double averageScore;
    private GradeLevel gradeLevel;

    // 공개된 시험만 시험일 오름차순으로 노출
    private List<ExamGradeDto> examGrades;

    @Getter
    @Builder
    public static class ExamGradeDto {
        private Long examId;
        private ExamType examType;
        private String examName;
        private LocalDate examDate;
        private Integer maxScore;
        private String coverage;            // 시험 범위
        private List<SubjectScoreDto> subjects;
    }

    @Getter
    @Builder
    public static class SubjectScoreDto {
        private String subjectName;
        private Integer rawScore;           // ABSENT면 null
        private ExamAttendanceStatus attendanceStatus;
    }
}
