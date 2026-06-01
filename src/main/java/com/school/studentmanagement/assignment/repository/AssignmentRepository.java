package com.school.studentmanagement.assignment.repository;

import com.school.studentmanagement.assignment.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    // 교사용: 특정 학급+과목의 과제 목록 (마감 최신순)
    @Query("SELECT a FROM Assignment a JOIN FETCH a.subject " +
            "WHERE a.classroom.id = :classroomId AND a.subject.id = :subjectId " +
            "ORDER BY a.dueDate DESC")
    List<Assignment> findByClassroomAndSubject(@Param("classroomId") Long classroomId,
                                               @Param("subjectId") Long subjectId);

    // 학생용: 본인 소속 학급의 과제 전체 (마감 최신순)
    @Query("SELECT a FROM Assignment a JOIN FETCH a.subject " +
            "WHERE a.classroom.id = :classroomId " +
            "ORDER BY a.dueDate DESC")
    List<Assignment> findByClassroomWithSubject(@Param("classroomId") Long classroomId);
}
