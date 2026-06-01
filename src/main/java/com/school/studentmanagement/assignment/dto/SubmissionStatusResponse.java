package com.school.studentmanagement.assignment.dto;

import com.school.studentmanagement.global.enums.SubmissionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

// 교사용: 특정 과제의 학급 학생별 제출 현황
@Getter
@Builder
public class SubmissionStatusResponse {

    private Long assignmentId;
    private String title;
    private LocalDateTime dueDate;
    private int totalStudents;
    private int submittedCount;
    private int lateCount;
    private List<StudentSubmissionDto> students;

    @Getter
    @Builder
    public static class StudentSubmissionDto {
        private Long studentId;
        private Integer studentNum;
        private String studentName;
        private SubmissionStatus status;   // NOT_SUBMITTED / SUBMITTED / LATE
        private LocalDateTime submittedAt; // 미제출이면 null
        private String content;            // 제출 본문(교사 검토용). 미제출이면 null
        private Integer score;             // 채점 점수(0~100). 미채점이면 null
        private String feedback;           // 채점 피드백. 미채점/미작성이면 null
    }
}
