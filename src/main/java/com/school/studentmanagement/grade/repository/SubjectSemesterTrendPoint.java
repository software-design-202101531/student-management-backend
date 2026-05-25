package com.school.studentmanagement.grade.repository;

// 한 학생의 시계열 추이: (subject, year, semester)별 학기 가중평균 점수
public interface SubjectSemesterTrendPoint {
    Long getSubjectId();
    Integer getAcademicYear();
    Integer getSemester();
    Double getSemesterScore();
}
