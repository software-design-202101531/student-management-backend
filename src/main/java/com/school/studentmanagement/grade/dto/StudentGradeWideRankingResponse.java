package com.school.studentmanagement.grade.dto;

import com.school.studentmanagement.global.enums.GradeLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StudentGradeWideRankingResponse {

    private Integer academicYear;
    private Integer semester;
    private Integer grade;          // 본인 학년

    private Integer rank;           // 학년 내 석차 (null이면 미산출)
    private Integer studentCount;   // 학년 학생 수
    private Double averageScore;
    private GradeLevel gradeLevel;
}
