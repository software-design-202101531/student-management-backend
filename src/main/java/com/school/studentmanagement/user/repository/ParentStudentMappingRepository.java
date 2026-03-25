package com.school.studentmanagement.user.repository;

import com.school.studentmanagement.user.entity.ParentStudentMapping;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParentStudentMappingRepository extends JpaRepository<ParentStudentMapping, Long> {
}
