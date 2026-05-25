package com.school.studentmanagement.grade.repository;

// 한 학생의 한 학기 과목별 가중평균 점수 (0~100 환산)
public interface SubjectScoreAggregation {
    Long getSubjectId();
    Double getSubjectScore();
}
