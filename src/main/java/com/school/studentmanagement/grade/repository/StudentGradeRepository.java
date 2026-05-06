package com.school.studentmanagement.grade.repository;

import com.school.studentmanagement.grade.entity.StudentGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentGradeRepository extends JpaRepository<StudentGrade, Long> {

    // 특정 시험 + 과목의 성적을 학생 목록 기준으로 한번에 조회 (과목 담당 교사용)
    @Query("SELECT sg FROM StudentGrade sg " +
            "JOIN FETCH sg.student s JOIN FETCH s.user " +
            "WHERE sg.exam.id = :examId " +
            "AND sg.subject.id = :subjectId " +
            "AND sg.student.id IN :studentIds")
    List<StudentGrade> findByExamIdAndSubjectIdAndStudentIds(
            @Param("examId") Long examId,
            @Param("subjectId") Long subjectId,
            @Param("studentIds") List<Long> studentIds
    );

    // 특정 시험의 전 과목 성적을 학생 목록 기준으로 한번에 조회 (담임 교사용)
    @Query("SELECT sg FROM StudentGrade sg " +
            "JOIN FETCH sg.subject " +
            "JOIN FETCH sg.student s JOIN FETCH s.user " +
            "WHERE sg.exam.id = :examId " +
            "AND sg.student.id IN :studentIds")
    List<StudentGrade> findByExamIdAndStudentIds(
            @Param("examId") Long examId,
            @Param("studentIds") List<Long> studentIds
    );

    Optional<StudentGrade> findByStudentIdAndExamIdAndSubjectId(Long studentId, Long examId, Long subjectId);

    // 학기 통계 계산용: 해당 학기 전체 성적 합산
    @Query("SELECT COALESCE(SUM(sg.rawScore), 0) FROM StudentGrade sg " +
            "WHERE sg.student.id = :studentId " +
            "AND sg.exam.academicYear = :academicYear " +
            "AND sg.exam.semester = :semester")
    Integer sumTotalScoreByStudentAndSemester(
            @Param("studentId") Long studentId,
            @Param("academicYear") Integer academicYear,
            @Param("semester") Integer semester
    );

    // 학기 통계 계산용: 해당 학기 성적 개수
    @Query("SELECT COUNT(sg) FROM StudentGrade sg " +
            "WHERE sg.student.id = :studentId " +
            "AND sg.exam.academicYear = :academicYear " +
            "AND sg.exam.semester = :semester")
    Long countByStudentAndSemester(
            @Param("studentId") Long studentId,
            @Param("academicYear") Integer academicYear,
            @Param("semester") Integer semester
    );

    // 학생 본인 성적 조회: 특정 학기의 전 시험·과목 성적
    @Query("SELECT sg FROM StudentGrade sg " +
            "JOIN FETCH sg.subject " +
            "JOIN FETCH sg.exam " +
            "WHERE sg.student.id = :studentId " +
            "AND sg.exam.academicYear = :academicYear " +
            "AND sg.exam.semester = :semester")
    List<StudentGrade> findByStudentIdAndAcademicYearAndSemester(
            @Param("studentId") Long studentId,
            @Param("academicYear") Integer academicYear,
            @Param("semester") Integer semester
    );
}
