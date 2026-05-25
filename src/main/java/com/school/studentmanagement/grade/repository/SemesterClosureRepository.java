package com.school.studentmanagement.grade.repository;

import com.school.studentmanagement.grade.entity.SemesterClosure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SemesterClosureRepository extends JpaRepository<SemesterClosure, Long> {

    Optional<SemesterClosure> findByAcademicYearAndSemester(Integer academicYear, Integer semester);

    boolean existsByAcademicYearAndSemester(Integer academicYear, Integer semester);

    void deleteByAcademicYearAndSemester(Integer academicYear, Integer semester);
}
