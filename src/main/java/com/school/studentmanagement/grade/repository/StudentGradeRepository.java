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

    // 학기 통계용: 한 학생의 한 학기 과목별 가중평균 점수 (0~100 환산)
    //   ABSENT(rawScore=null) 행은 자동으로 제외 — 가중평균 분모/분자에서 빠짐.
    //   CHEATED(rawScore=0)은 0점으로 평균에 포함.
    @Query("SELECT sg.subject.id AS subjectId, " +
            "SUM(sg.rawScore * 100.0 / sg.exam.maxScore * sg.exam.weight) / SUM(sg.exam.weight) AS subjectScore " +
            "FROM StudentGrade sg " +
            "WHERE sg.student.id = :studentId " +
            "AND sg.exam.academicYear = :academicYear " +
            "AND sg.exam.semester = :semester " +
            "AND sg.exam.weight > 0 " +
            "AND sg.rawScore IS NOT NULL " +
            "GROUP BY sg.subject.id")
    List<SubjectScoreAggregation> aggregateSubjectScoresByStudentAndSemester(
            @Param("studentId") Long studentId,
            @Param("academicYear") Integer academicYear,
            @Param("semester") Integer semester
    );

    // 레이더·학급 평균용: 학급 학생들의 (학생, 과목)별 학기 가중평균
    @Query("SELECT sg.student.id AS studentId, sg.subject.id AS subjectId, " +
            "SUM(sg.rawScore * 100.0 / sg.exam.maxScore * sg.exam.weight) / SUM(sg.exam.weight) AS subjectScore " +
            "FROM StudentGrade sg " +
            "WHERE sg.student.id IN :studentIds " +
            "AND sg.exam.academicYear = :academicYear " +
            "AND sg.exam.semester = :semester " +
            "AND sg.exam.weight > 0 " +
            "AND sg.rawScore IS NOT NULL " +
            "GROUP BY sg.student.id, sg.subject.id")
    List<ClassSubjectScoreAggregation> aggregateSubjectScoresByStudentIdsAndSemester(
            @Param("studentIds") List<Long> studentIds,
            @Param("academicYear") Integer academicYear,
            @Param("semester") Integer semester
    );

    // 시계열용: (subject, year, semester)별 학기점수
    @Query("SELECT sg.subject.id AS subjectId, " +
            "sg.exam.academicYear AS academicYear, " +
            "sg.exam.semester AS semester, " +
            "SUM(sg.rawScore * 100.0 / sg.exam.maxScore * sg.exam.weight) / SUM(sg.exam.weight) AS semesterScore " +
            "FROM StudentGrade sg " +
            "WHERE sg.student.id = :studentId " +
            "AND sg.exam.weight > 0 " +
            "AND sg.rawScore IS NOT NULL " +
            "AND (sg.exam.academicYear * 10 + sg.exam.semester) BETWEEN :fromKey AND :toKey " +
            "GROUP BY sg.subject.id, sg.exam.academicYear, sg.exam.semester " +
            "ORDER BY sg.exam.academicYear ASC, sg.exam.semester ASC")
    List<SubjectSemesterTrendPoint> aggregateSubjectTrendByStudentAndRange(
            @Param("studentId") Long studentId,
            @Param("fromKey") Integer fromKey,
            @Param("toKey") Integer toKey
    );

    // 학급 통계용: ABSENT 제외한 점수만
    @Query("SELECT sg.rawScore FROM StudentGrade sg " +
            "WHERE sg.exam.id = :examId " +
            "AND sg.subject.id = :subjectId " +
            "AND sg.student.id IN :studentIds " +
            "AND sg.rawScore IS NOT NULL")
    List<Integer> findRawScoresByExamIdAndSubjectIdAndStudentIds(
            @Param("examId") Long examId,
            @Param("subjectId") Long subjectId,
            @Param("studentIds") List<Long> studentIds
    );

    // 학생 본인 성적 조회: published=true인 시험만
    @Query("SELECT sg FROM StudentGrade sg " +
            "JOIN FETCH sg.subject " +
            "JOIN FETCH sg.exam e " +
            "WHERE sg.student.id = :studentId " +
            "AND e.academicYear = :academicYear " +
            "AND e.semester = :semester " +
            "AND e.published = true " +
            "ORDER BY e.examDate ASC, e.id ASC")
    List<StudentGrade> findPublishedByStudentIdAndAcademicYearAndSemester(
            @Param("studentId") Long studentId,
            @Param("academicYear") Integer academicYear,
            @Param("semester") Integer semester
    );

    // 교사 종합 뷰용 (published 무관)
    @Query("SELECT sg FROM StudentGrade sg " +
            "JOIN FETCH sg.subject " +
            "JOIN FETCH sg.exam e " +
            "WHERE sg.student.id = :studentId " +
            "AND e.academicYear = :academicYear " +
            "AND e.semester = :semester " +
            "ORDER BY e.examDate ASC, e.id ASC")
    List<StudentGrade> findByStudentIdAndAcademicYearAndSemester(
            @Param("studentId") Long studentId,
            @Param("academicYear") Integer academicYear,
            @Param("semester") Integer semester
    );

    // 학기 마감용: 학기의 모든 (학생, 시험, 과목) 조합 식별자만
    @Query("SELECT sg.student.id AS studentId, " +
            "sg.exam.id AS examId, " +
            "sg.subject.id AS subjectId " +
            "FROM StudentGrade sg " +
            "WHERE sg.exam.academicYear = :academicYear " +
            "AND sg.exam.semester = :semester")
    List<ExistingGradeKey> findExistingKeysByAcademicYearAndSemester(
            @Param("academicYear") Integer academicYear,
            @Param("semester") Integer semester
    );

    interface ExistingGradeKey {
        Long getStudentId();
        Long getExamId();
        Long getSubjectId();
    }
}
