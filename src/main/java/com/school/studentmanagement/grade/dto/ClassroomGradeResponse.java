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
public class ClassroomGradeResponse {

    private Long examId;
    private Integer academicYear;
    private Integer semester;
    private ExamType examType;
    private String examName;
    private LocalDate examDate;
    private Integer maxScore;
    private List<StudentAllGradesDto> students;

    @Getter
    @Builder
    public static class StudentAllGradesDto {
        private Long studentId;
        private String studentName;
        private Integer studentNum;

        // 학기 누적: 과목별 학기 점수의 합/평균/등급
        private Double totalScore;
        private Double averageScore;
        private GradeLevel gradeLevel;

        // 해당 시험의 과목별 점수
        private List<SubjectScoreDto> subjectScores;
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
