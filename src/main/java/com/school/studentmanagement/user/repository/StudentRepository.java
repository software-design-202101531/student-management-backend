package com.school.studentmanagement.user.repository;

import com.school.studentmanagement.user.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student,Long> {
    Optional<Student> findById(Long id);
}
