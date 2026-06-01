package com.school.studentmanagement.parent.repository;

import com.school.studentmanagement.parent.entity.ParentStudentMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ParentStudentMappingRepository extends JpaRepository<ParentStudentMapping, Long> {

    boolean existsByParentIdAndStudentId(Long parentId, Long studentId);

    // 특정 학생과 연결된 학부모 PK 목록 (알림 수신자 산출용). Parent PK == User PK.
    @Query("SELECT psm.parent.id FROM ParentStudentMapping psm WHERE psm.student.id = :studentId")
    List<Long> findParentIdsByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT psm FROM ParentStudentMapping psm " +
            "JOIN psm.parent p " +
            "JOIN FETCH psm.student s " +
            "JOIN FETCH s.user " +
            "WHERE p.id = :parentId")
    List<ParentStudentMapping> findAllByParentIdWithStudent(@Param("parentId") Long parentId);
}
