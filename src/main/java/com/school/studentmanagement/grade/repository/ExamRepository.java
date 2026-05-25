package com.school.studentmanagement.grade.repository;

import com.school.studentmanagement.global.enums.ExamType;
import com.school.studentmanagement.grade.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ExamRepository extends JpaRepository<Exam, Long> {

    boolean existsByAcademicYearAndSemesterAndExamTypeAndName(
            Integer academicYear, Integer semester, ExamType examType, String name);

    Optional<Exam> findByAcademicYearAndSemesterAndExamTypeAndName(
            Integer academicYear, Integer semester, ExamType examType, String name);

    List<Exam> findByAcademicYearAndSemesterOrderByExamDateAscIdAsc(Integer academicYear, Integer semester);

    // 학기 마감용: weight > 0인 시험만 (학기 평균에 반영되는 시험)
    List<Exam> findByAcademicYearAndSemesterAndWeightGreaterThan(Integer academicYear, Integer semester, Double weight);

    // scheduler 자동 fallback용: 모든 (year, semester) 페어
    @Query("SELECT DISTINCT e.academicYear AS academicYear, e.semester AS semester FROM Exam e")
    List<SemesterKey> findAllDistinctSemesters();

    interface SemesterKey {
        Integer getAcademicYear();
        Integer getSemester();
    }
}
