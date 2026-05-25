package com.school.studentmanagement.grade.dto;

import com.school.studentmanagement.global.enums.ExamAttendanceStatus;
import com.school.studentmanagement.global.enums.ExamType;
import com.school.studentmanagement.global.enums.GradeLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class StudentOverviewResponse {

    private Long studentId;
    private String studentName;
    private Integer grade;
    private Integer classNum;
    private Integer studentNum;
    private Integer academicYear;
    private Integer semester;

    // 학기 누적 통계
    private Double totalScore;
    private Double averageScore;
    private GradeLevel gradeLevel;
    private Integer classRank;
    private Integer classSize;

    private List<SubjectSemesterScoreDto> subjectScores;
    private List<ExamResultDto> examResults;

    @Getter
    @Builder
    public static class SubjectSemesterScoreDto {
        private Long subjectId;
        private String subjectName;
        private Double semesterScore;
        private Double classAverage;
    }

    @Getter
    @Builder
    public static class ExamResultDto {
        private Long examId;
        private ExamType examType;
        private String examName;
        private LocalDate examDate;
        private Integer maxScore;
        private String coverage;
        private boolean published;
        private List<SubjectScoreDto> subjects;
    }

    @Getter
    @Builder
    public static class SubjectScoreDto {
        private Long gradeId;
        private String subjectName;
        private Integer rawScore;                        // ABSENT면 null
        private ExamAttendanceStatus attendanceStatus;
    }
}
