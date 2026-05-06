package com.school.studentmanagement.record.repository;

import com.school.studentmanagement.global.enums.RecordCategory;
import com.school.studentmanagement.record.entity.StudentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentRecordRepository extends JpaRepository<StudentRecord,Long> {
    // 특정 학생의 특정 연도/학기 기록을 단건으로 조회
    Optional<StudentRecord> findByStudentIdAndRecordCategoryAndAcademicYearAndSemester(
            Long studentId,
            RecordCategory category,
            Integer academicYear,
            Integer semester
    );

    // 과세특 탐색
    Optional<StudentRecord> findByStudentIdAndRecordCategoryAndSubjectIdAndAcademicYearAndSemester(
            Long studentId,
            RecordCategory category,
            Long SubjectId,
            Integer academicYear,
            Integer semester
    );

    // 학생 본인 조회: 특정 학기의 행특·과세특 전체 목록
    @Query("SELECT sr FROM StudentRecord sr " +
            "LEFT JOIN FETCH sr.subject " +
            "WHERE sr.student.id = :studentId " +
            "AND sr.academicYear = :academicYear " +
            "AND sr.semester = :semester")
    List<StudentRecord> findAllByStudentIdAndAcademicYearAndSemester(
            @Param("studentId") Long studentId,
            @Param("academicYear") Integer academicYear,
            @Param("semester") Integer semester
    );
}
