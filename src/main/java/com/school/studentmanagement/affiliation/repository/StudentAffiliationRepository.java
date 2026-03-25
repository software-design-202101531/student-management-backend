package com.school.studentmanagement.affiliation.repository;

import com.school.studentmanagement.affiliation.entity.StudentAffiliation;
import com.school.studentmanagement.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StudentAffiliationRepository extends JpaRepository<StudentAffiliation, Long> {

    // 학년, 반, 번호, 이름이 일치하고 상태가 'PENDING'인 User 엔티티를 찾아옵니더!
    @Query("SELECT u FROM StudentAffiliation sa " +
            "JOIN sa.student s " +
            "JOIN s.user u " +
            "JOIN sa.classroom c " +
            "WHERE c.academicYear = :academicYear " +
            "AND c.grade = :grade " +
            "AND c.classNum = :classNum " +
            "AND sa.studentNum = :studentNum " +
            "AND u.name = :name " +
            "AND u.status = 'PENDING' " +
            "AND u.role = 'STUDENT'")
    Optional<User> findPendingStudentUser(@Param("academicYear") Integer academicYear,
                                          @Param("grade") Integer grade,
                                          @Param("classNum") Integer classNum,
                                          @Param("studentNum") Integer studentNum,
                                          @Param("name") String name);
}