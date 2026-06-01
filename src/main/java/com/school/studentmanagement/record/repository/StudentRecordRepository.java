package com.school.studentmanagement.record.repository;

import com.school.studentmanagement.global.enums.RecordCategory;
import com.school.studentmanagement.record.entity.StudentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentRecordRepository extends JpaRepository<StudentRecord,Long> {

    // 행특(subject_id=NULL) 안전 삽입. 이미 존재하면 ON CONFLICT DO NOTHING으로 '예외 없이' 0행 반환
    // (rollback-only 함정 회피). 부분 유니크 인덱스 uk_behavior_record를 충돌 타겟으로 사용한다.
    // 반환값: 1=삽입됨, 0=이미 존재(경합 패배).
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "INSERT INTO student_records " +
            "(student_id, teacher_id, academic_year, semester, record_category, content, version, created_at, updated_at) " +
            "VALUES (:studentId, :teacherId, :academicYear, :semester, 'BEHAVIOR_OPINION', :content, 0, localtimestamp, localtimestamp) " +
            "ON CONFLICT (student_id, academic_year, semester, record_category) WHERE subject_id IS NULL " +
            "DO NOTHING",
            nativeQuery = true)
    int insertBehaviorIfAbsent(@Param("studentId") Long studentId,
                               @Param("teacherId") Long teacherId,
                               @Param("academicYear") int academicYear,
                               @Param("semester") int semester,
                               @Param("content") String content);
    // 특정 학생의 특정 연도/학기 기록을 단건으로 조회
    Optional<StudentRecord> findByStudentIdAndRecordCategoryAndAcademicYearAndSemester(
            Long studentId,
            RecordCategory category,
            Integer academicYear,
            Integer semester
    );

    // 과세특 탐색
    Optional<StudentRecord> findByStudentIdAndRecordCategoryAndSubjectIdAndAcademicYearAndSemester(
            Long studentId,
            RecordCategory category,
            Long subjectId,
            Integer academicYear,
            Integer semester
    );

    // 학생 본인 조회: 특정 학기의 행특·과세특 전체 목록
    @Query("SELECT sr FROM StudentRecord sr " +
            "LEFT JOIN FETCH sr.subject " +
            "WHERE sr.student.id = :studentId " +
            "AND sr.academicYear = :academicYear " +
            "AND sr.semester = :semester")
    List<StudentRecord> findAllByStudentIdAndAcademicYearAndSemester(
            @Param("studentId") Long studentId,
            @Param("academicYear") Integer academicYear,
            @Param("semester") Integer semester
    );
}
