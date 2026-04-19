package com.school.studentmanagement.grade.repository;

import com.school.studentmanagement.global.enums.ExamType;
import com.school.studentmanagement.grade.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExamRepository extends JpaRepository<Exam, Long> {

    Optional<Exam> findByAcademicYearAndSemesterAndExamType(Integer academicYear, Integer semester, ExamType examType);
}
