package com.school.studentmanagement.grade.repository;

// 학급 학생들의 (학생, 과목)별 학기 가중평균 점수
public interface ClassSubjectScoreAggregation {
    Long getStudentId();
    Long getSubjectId();
    Double getSubjectScore();
}
