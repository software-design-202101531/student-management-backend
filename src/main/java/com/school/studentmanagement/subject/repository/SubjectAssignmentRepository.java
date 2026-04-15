package com.school.studentmanagement.subject.repository;

import com.school.studentmanagement.subject.entity.SubjectAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SubjectAssignmentRepository extends JpaRepository<SubjectAssignment,Long> {

    @Query("SELECT sa FROM SubjectAssignment sa " +
            "JOIN FETCH sa.classroom " +
            "JOIN FETCH sa.subject " +
            "JOIN FETCH sa.teacher " +
            "WHERE sa.teacher.id = :teacherId " +
            "AND sa.classroom.id = :classroomId " +
            "AND sa.subject.id = :subjectId " +
            "AND sa.academicYear = :year " +
            "AND sa.semester = :semester ")
    Optional<SubjectAssignment> findValidAssignment(
            @Param("teacherId") Long teacherId,
            @Param("classroomId") Long classroomId,
            @Param("subjectId") Long subjectId,
            @Param("year") Integer year,
            @Param("semester") Integer semester
    );
}
