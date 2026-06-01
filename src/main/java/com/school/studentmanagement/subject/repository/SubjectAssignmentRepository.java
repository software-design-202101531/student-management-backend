package com.school.studentmanagement.subject.repository;

import com.school.studentmanagement.subject.entity.SubjectAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubjectAssignmentRepository extends JpaRepository<SubjectAssignment,Long> {

    // classroom, subject, teacher 엔티티 조회
    @Query("SELECT sa FROM SubjectAssignment sa " +
            "JOIN FETCH sa.classroom " +
            "JOIN FETCH sa.subject " +
            "JOIN FETCH sa.teacher " +
            "WHERE sa.teacher.id = :teacherId " +
            "AND sa.classroom.id = :classroomId " +
            "AND sa.subject.id = :subjectId " +
            "AND sa.academicYear = :year " +
            "AND sa.semester = :semester ")
    Optional<SubjectAssignment> findValidAssignment(
            @Param("teacherId") Long teacherId,
            @Param("classroomId") Long classroomId,
            @Param("subjectId") Long subjectId,
            @Param("year") Integer year,
            @Param("semester") Integer semester
    );

    // 담당 과목 리스트 한번에 가져오기
    @Query("SELECT sa FROM SubjectAssignment sa " +
            "JOIN FETCH sa.classroom c " +
            "JOIN FETCH sa.subject s " +
            "WHERE sa.teacher.id = :teacherId " +
            "AND sa.academicYear = :year " +
            "AND sa.semester = :semester " +
            "ORDER BY c.grade ASC, c.classNum")
    List<SubjectAssignment> findAllMyAssignments(
            @Param("teacherId") Long teacherId,
            @Param("year") Integer year,
            @Param("semester") Integer semester
    );

    // 특정 교사가 해당 학년도/학기에 그 반을 담당하는지 존재 여부
    @Query("SELECT CASE WHEN COUNT(sa) > 0 THEN true ELSE false END " +
            "FROM SubjectAssignment sa " +
            "WHERE sa.teacher.id = :teacherId " +
            "AND sa.classroom.id = :classroomId " +
            "AND sa.academicYear = :year " +
            "AND sa.semester = :semester ")
    boolean existsByTeacherIdAndClassroomIdAndAcademicYearAndSemester(
            @Param("teacherId") Long teacherId,
            @Param("classroomId") Long classroomId,
            @Param("year") Integer year,
            @Param("semester") Integer semester
    );

    // 학기 마감용: 그 학기의 모든 SubjectAssignment (학급, 과목 조합)
    @Query("SELECT sa FROM SubjectAssignment sa " +
            "JOIN FETCH sa.classroom " +
            "JOIN FETCH sa.subject " +
            "WHERE sa.academicYear = :year AND sa.semester = :semester")
    List<SubjectAssignment> findAllByAcademicYearAndSemester(
            @Param("year") Integer year,
            @Param("semester") Integer semester
    );
}
