package com.school.studentmanagement.consultation.repository;

import com.school.studentmanagement.consultation.entity.Consultation;
import com.school.studentmanagement.global.enums.ConsultationVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

    // 알림 생성용 단건 조회 — 작성 교사·학생·담임 교사를 함께 로딩(별도 @Async 트랜잭션에서 지연로딩 회피).
    @Query("SELECT c FROM Consultation c " +
            "JOIN FETCH c.teacher t JOIN FETCH t.user " +
            "JOIN FETCH c.student s JOIN FETCH s.user " +
            "LEFT JOIN FETCH s.homeroomTeacher " +
            "WHERE c.id = :id")
    Optional<Consultation> findByIdWithParticipants(@Param("id") Long id);

    // 특정 학생의 전체 상담 내역 (작성 교사·이름까지 함께 로딩, 상담 일시 내림차순)
    // 권한 필터링은 서비스 계층에서 수행
    @Query("SELECT c FROM Consultation c " +
            "JOIN FETCH c.teacher t " +
            "JOIN FETCH t.user " +
            "WHERE c.student.id = :studentId " +
            "ORDER BY c.consultationDate DESC")
    List<Consultation> findAllByStudentId(@Param("studentId") Long studentId);

    /**
     * 교사 간 상담 내역 검색.
     * - 필터(studentId/teacherId/visibility/keyword/from/to)는 모두 nullable — null이면 해당 조건 무시.
     * - 권한 필터는 DB에서 처리:
     *   1) 요청자가 관리자(:isAdmin = true)
     *   2) 공개 범위가 ALL_TEACHERS
     *   3) 작성자 본인 (c.teacher.id = :requesterId)
     *   4) 해당 학생의 담임 (c.student.homeroomTeacher.id = :requesterId)
     *   네 조건 OR 중 하나라도 만족하면 통과. 학생.담임 nullable은 OR 매칭 시 자동 false 처리.
     * - keyword: content + nextPlan 에 대해 LIKE 검색 (대소문자 무시).
     */
    @Query(value = "SELECT c FROM Consultation c " +
            "JOIN FETCH c.teacher t " +
            "JOIN FETCH t.user " +
            "WHERE (:isAdmin = TRUE " +
            "       OR c.visibility = com.school.studentmanagement.global.enums.ConsultationVisibility.ALL_TEACHERS " +
            "       OR c.teacher.id = :requesterId " +
            "       OR c.student.homeroomTeacher.id = :requesterId) " +
            "AND (:studentId IS NULL OR c.student.id = :studentId) " +
            "AND (:teacherId IS NULL OR c.teacher.id = :teacherId) " +
            "AND (:visibility IS NULL OR c.visibility = :visibility) " +
            "AND (:from IS NULL OR c.consultationDate >= :from) " +
            "AND (:to IS NULL OR c.consultationDate <= :to) " +
            "AND (:keyword IS NULL " +
            "     OR LOWER(CAST(c.content AS string)) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "     OR LOWER(CAST(c.nextPlan AS string)) LIKE LOWER(CONCAT('%', :keyword, '%')))",
            countQuery = "SELECT COUNT(c) FROM Consultation c " +
                    "WHERE (:isAdmin = TRUE " +
                    "       OR c.visibility = com.school.studentmanagement.global.enums.ConsultationVisibility.ALL_TEACHERS " +
                    "       OR c.teacher.id = :requesterId " +
                    "       OR c.student.homeroomTeacher.id = :requesterId) " +
                    "AND (:studentId IS NULL OR c.student.id = :studentId) " +
                    "AND (:teacherId IS NULL OR c.teacher.id = :teacherId) " +
                    "AND (:visibility IS NULL OR c.visibility = :visibility) " +
                    "AND (:from IS NULL OR c.consultationDate >= :from) " +
                    "AND (:to IS NULL OR c.consultationDate <= :to) " +
                    "AND (:keyword IS NULL " +
                    "     OR LOWER(CAST(c.content AS string)) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "     OR LOWER(CAST(c.nextPlan AS string)) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Consultation> searchConsultations(@Param("isAdmin") boolean isAdmin,
                                           @Param("requesterId") Long requesterId,
                                           @Param("studentId") Long studentId,
                                           @Param("teacherId") Long teacherId,
                                           @Param("visibility") ConsultationVisibility visibility,
                                           @Param("keyword") String keyword,
                                           @Param("from") LocalDateTime from,
                                           @Param("to") LocalDateTime to,
                                           Pageable pageable);
}
