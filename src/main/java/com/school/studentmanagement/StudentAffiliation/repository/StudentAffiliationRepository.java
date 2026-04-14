package com.school.studentmanagement.StudentAffiliation.repository;

import com.school.studentmanagement.StudentAffiliation.entity.StudentAffiliation;
import com.school.studentmanagement.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentAffiliationRepository extends JpaRepository<StudentAffiliation,Long> {

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

    // 담임인 반의 ID를 넣으면 소속된 학생들의 정보를 가져온다
    @Query("SELECT sa FROM StudentAffiliation sa " +
            "JOIN FETCH sa.student s " +
            "JOIN FETCH s.user " +
            "WHERE sa.classroom.id = :classroomId " +
            "ORDER BY sa.studentNum ASC")
    List<StudentAffiliation> findAllByClassroomId(@Param("classroomId") Long classroomId);

    // 연도, 학기 정보를 바탕으로 학생의 반 배정 정보를 확인
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
}
