package com.school.studentmanagement.grade.dto;

import com.school.studentmanagement.global.enums.ExamAttendanceStatus;
import com.school.studentmanagement.global.enums.ExamType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class GradeListResponse {

    private Long examId;
    private Integer academicYear;
    private Integer semester;
    private ExamType examType;
    private String examName;
    private LocalDate examDate;
    private Integer maxScore;
    private boolean published;
    private String subjectName;
    private List<StudentGradeDto> grades;

    @Getter
    @Builder
    public static class StudentGradeDto {
        private Long gradeId;                            // null이면 미입력 상태
        private Long studentId;
        private String studentName;
        private Integer studentNum;
        private Integer rawScore;                        // ABSENT or 미입력이면 null
        private ExamAttendanceStatus attendanceStatus;   // null이면 미입력 상태
    }
}
