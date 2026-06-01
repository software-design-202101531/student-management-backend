package com.school.studentmanagement.student.repository;

import com.school.studentmanagement.student.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    // 본인 프로필 조회용: 학생 + User + 담임 교사(+담임의 User)를 한 번에 로딩 (담임 미배정 시 null 허용)
    @Query("SELECT s FROM Student s " +
            "JOIN FETCH s.user " +
            "LEFT JOIN FETCH s.homeroomTeacher ht " +
            "LEFT JOIN FETCH ht.user " +
            "WHERE s.id = :id")
    Optional<Student> findByIdWithUserAndHomeroom(@Param("id") Long id);
}
