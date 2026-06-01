package com.school.studentmanagement.assignment.dto;

import com.school.studentmanagement.global.enums.SubmissionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 학생 본인이 받은 과제 + 제출 상태
@Getter
@Builder
public class StudentAssignmentResponse {

    private Long assignmentId;
    private String subjectName;
    private String title;
    private String description;         // 과제 지시문(학생이 볼 내용)
    private LocalDateTime dueDate;
    private SubmissionStatus status;   // NOT_SUBMITTED / SUBMITTED / LATE
    private LocalDateTime submittedAt; // 미제출이면 null
    private String content;            // 내가 제출한 본문(재확인용). 미제출이면 null
    private Integer score;             // 내 점수(0~100). 미채점이면 null
    private String feedback;           // 교사 피드백. 미채점/미작성이면 null
}
