package com.school.studentmanagement.subject.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TeacherAssignmentResponse {
    // 선생님이 담당 과목 교사인 반 반환
    private Long assignmentId;  // 배정 고유 ID
    private Long classroomId;   // 반 ID
    private Integer grade;      // 학년
    private Integer classNum;   // 반 번호
    private Long subjectId;     // 과목 ID
    private String subjectName; // 과목 이름
}
