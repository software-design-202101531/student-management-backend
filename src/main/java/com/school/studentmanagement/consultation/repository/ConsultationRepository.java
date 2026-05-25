package com.school.studentmanagement.consultation.repository;

import com.school.studentmanagement.consultation.entity.Consultation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

    // 특정 학생의 전체 상담 내역 (작성 교사·이름까지 함께 로딩, 상담 일시 내림차순)
    // 권한 필터링은 서비스 계층에서 수행
    @Query("SELECT c FROM Consultation c " +
            "JOIN FETCH c.teacher t " +
            "JOIN FETCH t.user " +
            "WHERE c.student.id = :studentId " +
            "ORDER BY c.consultationDate DESC")
    List<Consultation> findAllByStudentId(@Param("studentId") Long studentId);
}
