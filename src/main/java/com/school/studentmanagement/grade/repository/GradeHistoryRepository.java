package com.school.studentmanagement.grade.repository;

import com.school.studentmanagement.grade.entity.GradeHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GradeHistoryRepository extends JpaRepository<GradeHistory, Long> {

    List<GradeHistory> findByStudentGradeIdOrderByChangedAtDesc(Long studentGradeId);
}
