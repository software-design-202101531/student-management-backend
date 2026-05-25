package com.school.studentmanagement.grade.service;

import com.school.studentmanagement.classroom.entity.StudentAffiliation;
import com.school.studentmanagement.classroom.repository.StudentAffiliationRepository;
import com.school.studentmanagement.global.enums.ExamAttendanceStatus;
import com.school.studentmanagement.global.enums.SemesterClosureMethod;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.exception.ErrorCode;
import com.school.studentmanagement.global.util.AcademicCalendarUtil;
import com.school.studentmanagement.grade.dto.ClosePreviewResponse;
import com.school.studentmanagement.grade.dto.SemesterClosureResponse;
import com.school.studentmanagement.grade.entity.Exam;
import com.school.studentmanagement.grade.entity.SemesterClosure;
import com.school.studentmanagement.grade.entity.StudentGrade;
import com.school.studentmanagement.grade.repository.ExamRepository;
import com.school.studentmanagement.grade.repository.SemesterClosureRepository;
import com.school.studentmanagement.grade.repository.StudentGradeRepository;
import com.school.studentmanagement.student.entity.Student;
import com.school.studentmanagement.subject.entity.Subject;
import com.school.studentmanagement.subject.entity.SubjectAssignment;
import com.school.studentmanagement.subject.repository.SubjectAssignmentRepository;
import com.school.studentmanagement.user.entity.User;
import com.school.studentmanagement.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SemesterClosureService {

    private final SemesterClosureRepository semesterClosureRepository;
    private final ExamRepository examRepository;
    private final SubjectAssignmentRepository subjectAssignmentRepository;
    private final StudentAffiliationRepository studentAffiliationRepository;
    private final StudentGradeRepository studentGradeRepository;
    private final UserRepository userRepository;
    private final SemesterStatRecalculator semesterStatRecalculator;
    private final AcademicCalendarUtil academicCalendarUtil;

    // ─── 상태 조회 ───────────────────────────────────────────────────────────

    public SemesterClosureResponse getStatus(int academicYear, int semester) {
        return semesterClosureRepository.findByAcademicYearAndSemester(academicYear, semester)
                .map(SemesterClosureResponse::closed)
                .orElseGet(() -> SemesterClosureResponse.open(academicYear, semester));
    }

    public boolean isClosed(int academicYear, int semester) {
        return semesterClosureRepository.existsByAcademicYearAndSemester(academicYear, semester);
    }

    // ─── 마감 미리보기 (dry-run) ─────────────────────────────────────────────

    public ClosePreviewResponse preview(int academicYear, int semester) {
        if (isClosed(academicYear, semester)) {
            throw new BusinessException(ErrorCode.SEMESTER_ALREADY_CLOSED);
        }
        List<MissingCombination> missing = findMissingCombinations(academicYear, semester);
        return buildPreviewResponse(academicYear, semester, missing);
    }

    // ─── 마감 (수동) ─────────────────────────────────────────────────────────

    @Transactional
    public SemesterClosureResponse close(int academicYear, int semester, Long userId, String reason) {
        return doClose(academicYear, semester, userId, reason, SemesterClosureMethod.MANUAL);
    }

    // ─── 마감 (자동 fallback) ────────────────────────────────────────────────

    @Transactional
    public SemesterClosureResponse autoClose(int academicYear, int semester) {
        return doClose(academicYear, semester, null,
                "자동 마감 (마감일 경과)", SemesterClosureMethod.AUTO_FALLBACK);
    }

    private SemesterClosureResponse doClose(int academicYear, int semester, Long userId, String reason,
                                            SemesterClosureMethod method) {
        if (isClosed(academicYear, semester)) {
            throw new BusinessException(ErrorCode.SEMESTER_ALREADY_CLOSED);
        }

        List<MissingCombination> missing = findMissingCombinations(academicYear, semester);

        // 누락 row INSERT (NOT_SUBMITTED, rawScore=0 — entity가 강제 정규화)
        Set<Long> affectedStudentIds = new HashSet<>();
        for (MissingCombination m : missing) {
            studentGradeRepository.save(StudentGrade.builder()
                    .student(m.student)
                    .exam(m.exam)
                    .subject(m.subject)
                    .attendanceStatus(ExamAttendanceStatus.NOT_SUBMITTED)
                    .build());
            affectedStudentIds.add(m.student.getId());
        }

        // 영향받은 학생 stat 재계산
        Map<Long, Student> studentById = missing.stream()
                .collect(Collectors.toMap(m -> m.student.getId(), m -> m.student, (a, b) -> a));
        for (Long studentId : affectedStudentIds) {
            semesterStatRecalculator.refresh(studentById.get(studentId), academicYear, semester);
        }

        // closure 저장
        String closedByName = method == SemesterClosureMethod.AUTO_FALLBACK
                ? "system"
                : userRepository.findById(userId).map(User::getName).orElse("(unknown)");

        SemesterClosure closure = semesterClosureRepository.save(SemesterClosure.builder()
                .academicYear(academicYear)
                .semester(semester)
                .method(method)
                .closedByUserId(method == SemesterClosureMethod.MANUAL ? userId : null)
                .closedByName(closedByName)
                .reason(reason)
                .filledCount(missing.size())
                .build());

        return SemesterClosureResponse.closed(closure);
    }

    // ─── 재개방 ──────────────────────────────────────────────────────────────

    @Transactional
    public void reopen(int academicYear, int semester) {
        SemesterClosure closure = semesterClosureRepository
                .findByAcademicYearAndSemester(academicYear, semester)
                .orElseThrow(() -> new BusinessException(ErrorCode.SEMESTER_NOT_CLOSED));
        // NOT_SUBMITTED row는 그대로 두고 closure만 삭제 — 교사가 update API로 PRESENT/점수 수정 가능
        semesterClosureRepository.delete(closure);
    }

    // ─── 자동 fallback (scheduler 진입점) ────────────────────────────────────

    @Transactional
    public int autoCloseExpired() {
        List<ExamRepository.SemesterKey> all = examRepository.findAllDistinctSemesters();
        int closedCount = 0;
        for (ExamRepository.SemesterKey key : all) {
            int year = key.getAcademicYear();
            int sem = key.getSemester();
            if (academicCalendarUtil.isModifiable(year)) continue;
            if (isClosed(year, sem)) continue;
            autoClose(year, sem);
            closedCount++;
        }
        return closedCount;
    }

    // ─── 누락 조합 산출 (preview/close 공통) ─────────────────────────────────

    private List<MissingCombination> findMissingCombinations(int academicYear, int semester) {
        List<Exam> exams = examRepository
                .findByAcademicYearAndSemesterAndWeightGreaterThan(academicYear, semester, 0.0);
        if (exams.isEmpty()) return List.of();

        List<SubjectAssignment> assignments = subjectAssignmentRepository
                .findAllByAcademicYearAndSemester(academicYear, semester);
        if (assignments.isEmpty()) return List.of();

        // 학급별 학생 명단 — 학급당 1쿼리지만 학기 마감은 빈도 낮은 작업이라 허용
        Set<Long> classroomIds = assignments.stream()
                .map(a -> a.getClassroom().getId())
                .collect(Collectors.toSet());
        Map<Long, List<StudentAffiliation>> studentsByClassroom = new HashMap<>();
        for (Long classroomId : classroomIds) {
            studentsByClassroom.put(classroomId,
                    studentAffiliationRepository.findAllByClassroomId(classroomId));
        }

        // 기존 (학생, 시험, 과목) 조합 한 번에
        Set<String> existingKeys = studentGradeRepository
                .findExistingKeysByAcademicYearAndSemester(academicYear, semester)
                .stream()
                .map(k -> key(k.getStudentId(), k.getExamId(), k.getSubjectId()))
                .collect(Collectors.toSet());

        // 기대 - 기존 = 누락
        List<MissingCombination> missing = new ArrayList<>();
        for (SubjectAssignment sa : assignments) {
            Subject subject = sa.getSubject();
            List<StudentAffiliation> students = studentsByClassroom
                    .getOrDefault(sa.getClassroom().getId(), List.of());

            for (StudentAffiliation aff : students) {
                Student student = aff.getStudent();
                for (Exam exam : exams) {
                    String k = key(student.getId(), exam.getId(), subject.getId());
                    if (!existingKeys.contains(k)) {
                        missing.add(new MissingCombination(student, exam, subject, aff));
                    }
                }
            }
        }
        return missing;
    }

    private static String key(Long studentId, Long examId, Long subjectId) {
        return studentId + ":" + examId + ":" + subjectId;
    }

    private ClosePreviewResponse buildPreviewResponse(int year, int semester, List<MissingCombination> missing) {
        Map<Long, List<MissingCombination>> byStudent = missing.stream()
                .collect(Collectors.groupingBy(m -> m.student.getId()));

        List<ClosePreviewResponse.StudentMissing> studentDtos = byStudent.values().stream()
                .map(list -> {
                    MissingCombination first = list.get(0);
                    Student student = first.student;
                    StudentAffiliation aff = first.affiliation;
                    List<ClosePreviewResponse.MissingEntry> entries = list.stream()
                            .map(m -> ClosePreviewResponse.MissingEntry.builder()
                                    .examId(m.exam.getId())
                                    .examName(m.exam.getName())
                                    .subjectId(m.subject.getId())
                                    .subjectName(m.subject.getName())
                                    .build())
                            .toList();
                    return ClosePreviewResponse.StudentMissing.builder()
                            .studentId(student.getId())
                            .studentName(student.getUser().getName())
                            .grade(aff.getClassroom().getGrade())
                            .classNum(aff.getClassroom().getClassNum())
                            .studentNum(aff.getStudentNum())
                            .missing(entries)
                            .build();
                })
                .toList();

        return ClosePreviewResponse.builder()
                .academicYear(year)
                .semester(semester)
                .totalMissingCount(missing.size())
                .affectedStudentCount(byStudent.size())
                .students(studentDtos)
                .build();
    }

    // 내부 record (Java 17+)
    private record MissingCombination(Student student, Exam exam, Subject subject, StudentAffiliation affiliation) {}
}
