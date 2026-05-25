package com.school.studentmanagement.feedback.repository;

import com.school.studentmanagement.feedback.entity.Feedback;
import com.school.studentmanagement.global.enums.FeedbackStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    // 교사용: 해당 학생의 모든 피드백 (status/is_public 조건 없이 전체)
    @Query("SELECT f FROM Feedback f " +
            "JOIN FETCH f.teacher t " +
            "JOIN FETCH t.user " +
            "WHERE f.student.id = :studentId " +
            "ORDER BY f.createdAt DESC")
    List<Feedback> findAllByStudentId(@Param("studentId") Long studentId);

    // 학생/학부모용: 발행 완료(PUBLISHED) + 공개(is_public = true) 피드백만
    @Query("SELECT f FROM Feedback f " +
            "JOIN FETCH f.teacher t " +
            "JOIN FETCH t.user " +
            "WHERE f.student.id = :studentId " +
            "AND f.status = :status " +
            "AND f.isPublic = true " +
            "ORDER BY f.createdAt DESC")
    List<Feedback> findVisibleByStudentId(@Param("studentId") Long studentId,
                                          @Param("status") FeedbackStatus status);
}
