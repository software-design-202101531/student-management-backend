package com.school.studentmanagement.teacher.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TeacherAssignmentResponse {
    private Long assignmentId;
    private Long classroomId;
    private Integer grade;
    private Integer classNum;
    private Long subjectId;
    private String subjectName;
}
