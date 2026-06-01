package com.school.studentmanagement.feedback.repository;

import com.school.studentmanagement.feedback.entity.Feedback;
import com.school.studentmanagement.global.enums.FeedbackCategory;
import com.school.studentmanagement.global.enums.FeedbackStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    // 교사용: 발행 완료(PUBLISHED) 건 전체 + 본인이 작성한 건(초안 포함). 타 교사의 초안은 제외.
    @Query("SELECT f FROM Feedback f " +
            "JOIN FETCH f.teacher t " +
            "JOIN FETCH t.user " +
            "WHERE f.student.id = :studentId " +
            "AND (f.status = :status OR f.teacher.id = :teacherId) " +
            "ORDER BY f.createdAt DESC")
    List<Feedback> findForTeacherView(@Param("studentId") Long studentId,
                                      @Param("status") FeedbackStatus status,
                                      @Param("teacherId") Long teacherId);

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

    // ----- 페이지·필터(카테고리/기간) 변형 -----
    // 모든 기간/카테고리 파라미터는 nullable. null이면 해당 조건은 무시(IS NULL OR ...)
    // Pageable의 sort는 컨트롤러 측 @PageableDefault로 createdAt DESC 기본값 부여.

    // 교사용 + 필터 + 페이징
    @Query(value = "SELECT f FROM Feedback f " +
            "JOIN FETCH f.teacher t " +
            "JOIN FETCH t.user " +
            "WHERE f.student.id = :studentId " +
            "AND (f.status = :status OR f.teacher.id = :teacherId) " +
            "AND (:category IS NULL OR f.category = :category) " +
            "AND (:from IS NULL OR f.createdAt >= :from) " +
            "AND (:to IS NULL OR f.createdAt <= :to)",
            countQuery = "SELECT COUNT(f) FROM Feedback f " +
                    "WHERE f.student.id = :studentId " +
                    "AND (f.status = :status OR f.teacher.id = :teacherId) " +
                    "AND (:category IS NULL OR f.category = :category) " +
                    "AND (:from IS NULL OR f.createdAt >= :from) " +
                    "AND (:to IS NULL OR f.createdAt <= :to)")
    Page<Feedback> searchForTeacherView(@Param("studentId") Long studentId,
                                        @Param("status") FeedbackStatus status,
                                        @Param("teacherId") Long teacherId,
                                        @Param("category") FeedbackCategory category,
                                        @Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to,
                                        Pageable pageable);

    // 학생/학부모용 + 필터 + 페이징 (PUBLISHED + isPublic 고정)
    @Query(value = "SELECT f FROM Feedback f " +
            "JOIN FETCH f.teacher t " +
            "JOIN FETCH t.user " +
            "WHERE f.student.id = :studentId " +
            "AND f.status = :status " +
            "AND f.isPublic = true " +
            "AND (:category IS NULL OR f.category = :category) " +
            "AND (:from IS NULL OR f.createdAt >= :from) " +
            "AND (:to IS NULL OR f.createdAt <= :to)",
            countQuery = "SELECT COUNT(f) FROM Feedback f " +
                    "WHERE f.student.id = :studentId " +
                    "AND f.status = :status " +
                    "AND f.isPublic = true " +
                    "AND (:category IS NULL OR f.category = :category) " +
                    "AND (:from IS NULL OR f.createdAt >= :from) " +
                    "AND (:to IS NULL OR f.createdAt <= :to)")
    Page<Feedback> searchVisibleByStudentId(@Param("studentId") Long studentId,
                                            @Param("status") FeedbackStatus status,
                                            @Param("category") FeedbackCategory category,
                                            @Param("from") LocalDateTime from,
                                            @Param("to") LocalDateTime to,
                                            Pageable pageable);
}
