package com.school.studentmanagement.assignment.dto;

import com.school.studentmanagement.assignment.entity.Assignment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AssignmentResponse {

    private Long assignmentId;
    private String subjectName;
    private String title;
    private String description;
    private LocalDateTime dueDate;
    private LocalDateTime createdAt;

    public static AssignmentResponse from(Assignment a) {
        return AssignmentResponse.builder()
                .assignmentId(a.getId())
                .subjectName(a.getSubject().getName())
                .title(a.getTitle())
                .description(a.getDescription())
                .dueDate(a.getDueDate())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
