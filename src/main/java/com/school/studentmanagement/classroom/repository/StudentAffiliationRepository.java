package com.school.studentmanagement.classroom.repository;

import com.school.studentmanagement.classroom.entity.StudentAffiliation;
import com.school.studentmanagement.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentAffiliationRepository extends JpaRepository<StudentAffiliation, Long> {

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

    @Query("SELECT sa FROM StudentAffiliation sa " +
            "JOIN FETCH sa.student s " +
            "JOIN FETCH s.user " +
            "WHERE sa.classroom.id = :classroomId " +
            "ORDER BY sa.studentNum ASC")
    List<StudentAffiliation> findAllByClassroomId(@Param("classroomId") Long classroomId);

    @Query("SELECT sa FROM StudentAffiliation sa " +
            "JOIN FETCH sa.student s " +
            "JOIN FETCH s.user u " +
            "JOIN FETCH sa.classroom c " +
            "WHERE s.id = :studentId " +
            "AND c.academicYear = :year " +
            "AND c.semester = :semester")
    Optional<StudentAffiliation> findWithAllDetails(
            @Param("studentId") Long studentId,
            @Param("year") Integer year,
            @Param("semester") Integer semester
    );

    Optional<StudentAffiliation> findByStudentIdAndClassroomId(Long studentId, Long classroomId);
}
