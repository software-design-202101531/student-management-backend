package com.school.studentmanagement.parent.repository;

import com.school.studentmanagement.parent.entity.ParentStudentMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ParentStudentMappingRepository extends JpaRepository<ParentStudentMapping, Long> {

    boolean existsByParentIdAndStudentId(Long parentId, Long studentId);

    @Query("SELECT psm FROM ParentStudentMapping psm " +
            "JOIN psm.parent p " +
            "JOIN FETCH psm.student s " +
            "JOIN FETCH s.user " +
            "WHERE p.id = :parentId")
    List<ParentStudentMapping> findAllByParentIdWithStudent(@Param("parentId") Long parentId);
}
