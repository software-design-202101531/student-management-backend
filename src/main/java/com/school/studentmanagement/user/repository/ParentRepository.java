package com.school.studentmanagement.user.repository;

import com.school.studentmanagement.user.entity.Parent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParentRepository extends JpaRepository<Parent, Long> {
}
