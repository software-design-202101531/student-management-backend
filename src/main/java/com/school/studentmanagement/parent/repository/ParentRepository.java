package com.school.studentmanagement.parent.repository;

import com.school.studentmanagement.parent.entity.Parent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ParentRepository extends JpaRepository<Parent, Long> {

    // 본인 프로필 조회용: 학부모 + User를 한 번에 로딩
    @Query("SELECT p FROM Parent p JOIN FETCH p.user WHERE p.id = :id")
    Optional<Parent> findByIdWithUser(@Param("id") Long id);
}
