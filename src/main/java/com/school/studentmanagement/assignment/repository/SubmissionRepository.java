package com.school.studentmanagement.assignment.repository;

import com.school.studentmanagement.assignment.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    Optional<Submission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    // 과제 삭제 시 제출들 일괄 제거
    void deleteByAssignmentId(Long assignmentId);

    // 특정 과제의 제출 목록 (제출 현황 집계용)
    List<Submission> findByAssignmentId(Long assignmentId);

    // 학생용: 여러 과제에 대한 본인 제출들 (목록 화면에서 상태 매핑)
    @Query("SELECT s FROM Submission s WHERE s.student.id = :studentId AND s.assignment.id IN :assignmentIds")
    List<Submission> findByStudentAndAssignments(@Param("studentId") Long studentId,
                                                 @Param("assignmentIds") List<Long> assignmentIds);
}
