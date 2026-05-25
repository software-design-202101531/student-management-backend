package com.school.studentmanagement.grade.repository;

import com.school.studentmanagement.grade.entity.GradePolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GradePolicyRepository extends JpaRepository<GradePolicy, Long> {

    Optional<GradePolicy> findFirstByActiveTrue();
}
