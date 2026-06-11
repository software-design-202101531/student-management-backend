package com.school.studentmanagement.grade.service;

import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.exception.ErrorCode;
import com.school.studentmanagement.global.validation.TeacherStudentRelationValidator;
import com.school.studentmanagement.grade.dto.GradeSearchResponse;
import com.school.studentmanagement.grade.entity.StudentGrade;
import com.school.studentmanagement.grade.repository.StudentGradeRepository;
import com.school.studentmanagement.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 성적 검색 — 한 학생의 개별 시험 성적을 과목·학기 범위로 필터링한다.
 * - 교사: 담임/과목담당만, 미발행 포함
 * - 학생 본인: 발행분만
 * 학기 범위는 (academicYear*10 + semester) 키로 비교한다(기존 trend 쿼리와 동일 규약).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GradeSearchService {

    // 학기는 1·2학기만 존재 → 연도만 지정된 범위 경계의 기본 보정값
    private static final int MIN_SEMESTER = 1;
    private static final int MAX_SEMESTER = 2;

    private final StudentGradeRepository studentGradeRepository;
    private final StudentRepository studentRepository;
    private final TeacherStudentRelationValidator teacherStudentRelationValidator;

    // 교사용: 특정 학생 성적 검색 (담임 또는 과목 담당 교사만, 미발행 시험 포함)
    public GradeSearchResponse searchForTeacher(Long studentId, Long teacherId, Long subjectId,
                                                Integer fromYear, Integer fromSemester,
                                                Integer toYear, Integer toSemester) {
        ensureStudentExists(studentId);
        teacherStudentRelationValidator.validateCanWriteFor(teacherId, studentId);
        return search(studentId, subjectId, fromYear, fromSemester, toYear, toSemester, true);
    }

    // 학생 본인용: 발행된 시험 성적만
    public GradeSearchResponse searchForStudent(Long studentId, Long subjectId,
                                                Integer fromYear, Integer fromSemester,
                                                Integer toYear, Integer toSemester) {
        ensureStudentExists(studentId);
        return search(studentId, subjectId, fromYear, fromSemester, toYear, toSemester, false);
    }

    private GradeSearchResponse search(Long studentId, Long subjectId,
                                       Integer fromYear, Integer fromSemester,
                                       Integer toYear, Integer toSemester,
                                       boolean includeUnpublished) {
        Integer fromKey = toKey(fromYear, fromSemester, MIN_SEMESTER);
        Integer toKey = toKey(toYear, toSemester, MAX_SEMESTER);

        List<StudentGrade> grades = studentGradeRepository.searchByStudentAndFilters(
                studentId, subjectId, fromKey, toKey, includeUnpublished);
        return GradeSearchResponse.from(studentId, grades);
    }

    // (연도, 학기) → 비교 키(year*10+semester). 연도 미지정이면 null(해당 방향 무한대),
    // 연도만 지정되면 학기는 경계 기본값(시작=1, 끝=2)으로 보정한다.
    private Integer toKey(Integer year, Integer semester, int defaultSemester) {
        if (year == null) {
            return null;
        }
        int sem = (semester != null) ? semester : defaultSemester;
        return year * 10 + sem;
    }

    private void ensureStudentExists(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new BusinessException(ErrorCode.STUDENT_NOT_FOUND);
        }
    }
}
