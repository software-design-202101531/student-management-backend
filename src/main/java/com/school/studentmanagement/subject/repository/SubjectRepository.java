package com.school.studentmanagement.subject.repository;

import com.school.studentmanagement.subject.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectRepository extends JpaRepository<Subject,Long> {
}
