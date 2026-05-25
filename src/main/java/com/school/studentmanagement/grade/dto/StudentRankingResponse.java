package com.school.studentmanagement.grade.dto;

import com.school.studentmanagement.global.enums.GradeLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StudentRankingResponse {

    private Integer academicYear;
    private Integer semester;

    private Integer rank;          // null이면 미산출 (학기 통계 없음)
    private Integer classSize;     // 학급 학생 수
    private Double averageScore;
    private GradeLevel gradeLevel;
}
