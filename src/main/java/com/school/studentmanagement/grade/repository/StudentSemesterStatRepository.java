package com.school.studentmanagement.grade.repository;

import com.school.studentmanagement.grade.entity.StudentSemesterStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentSemesterStatRepository extends JpaRepository<StudentSemesterStat, Long> {

    Optional<StudentSemesterStat> findByStudentIdAndAcademicYearAndSemester(
            Long studentId, Integer academicYear, Integer semester
    );

    // 담임 교사 전체 조회용: 학급 학생들의 학기 통계 일괄 조회
    @Query("SELECT sss FROM StudentSemesterStat sss " +
            "JOIN FETCH sss.student s JOIN FETCH s.user " +
            "WHERE sss.student.id IN :studentIds " +
            "AND sss.academicYear = :academicYear " +
            "AND sss.semester = :semester")
    List<StudentSemesterStat> findByStudentIdsAndYearAndSemester(
            @Param("studentIds") List<Long> studentIds,
            @Param("academicYear") Integer academicYear,
            @Param("semester") Integer semester
    );
}
