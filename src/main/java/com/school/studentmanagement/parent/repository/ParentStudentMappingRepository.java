package com.school.studentmanagement.parent.repository;

import com.school.studentmanagement.parent.entity.ParentStudentMapping;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParentStudentMappingRepository extends JpaRepository<ParentStudentMapping, Long> {
}
