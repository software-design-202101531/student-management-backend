package com.school.studentmanagement.user.repository;

import com.school.studentmanagement.user.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    Optional<Teacher> findTeacherById(Long id);

    @Query("SELECT t FROM Teacher t " +
            "JOIN FETCH t.user " +
            "JOIN FETCH t.subject " +
            "WHERE t.id = :id ")
    Optional<Teacher> findByIdwithDetails(@Param("id") Long id);
}
