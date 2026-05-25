package com.school.studentmanagement.grade.service;

import com.school.studentmanagement.classroom.entity.StudentAffiliation;
import com.school.studentmanagement.classroom.repository.ClassRoomRepository;
import com.school.studentmanagement.classroom.repository.StudentAffiliationRepository;
import com.school.studentmanagement.global.enums.ExamAttendanceStatus;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.exception.ErrorCode;
import com.school.studentmanagement.grade.dto.ClassroomGradeResponse;
import com.school.studentmanagement.grade.dto.GradeListResponse;
import com.school.studentmanagement.grade.dto.GradeSaveRequest;
import com.school.studentmanagement.grade.dto.GradeUpdateRequest;
import com.school.studentmanagement.grade.entity.Exam;
import com.school.studentmanagement.grade.entity.GradeHistory;
import com.school.studentmanagement.grade.entity.StudentGrade;
import com.school.studentmanagement.grade.entity.StudentSemesterStat;
import com.school.studentmanagement.grade.repository.ExamRepository;
import com.school.studentmanagement.grade.repository.GradeHistoryRepository;
import com.school.studentmanagement.grade.repository.StudentGradeRepository;
import com.school.studentmanagement.grade.repository.StudentSemesterStatRepository;
import com.school.studentmanagement.student.entity.Student;
import com.school.studentmanagement.subject.entity.SubjectAssignment;
import com.school.studentmanagement.subject.repository.SubjectAssignmentRepository;
import com.school.studentmanagement.user.entity.User;
import com.school.studentmanagement.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentGradeService {

    private final StudentGradeRepository studentGradeRepository;
    private final StudentSemesterStatRepository semesterStatRepository;
    private final ExamRepository examRepository;
    private final SubjectAssignmentRepository subjectAssignmentRepository;
    private final StudentAffiliationRepository studentAffiliationRepository;
    private final ClassRoomRepository classRoomRepository;
    private final GradeHistoryRepository gradeHistoryRepository;
    private final UserRepository userRepository;
    private final SemesterStatRecalculator semesterStatRecalculator;
    private final SemesterClosureService semesterClosureService;

    // ─── 성적 입력 (과목 담당 교사) ──────────────────────────────────────────

    @Transactional
    public void saveGrades(Long classroomId, Long subjectId, Long teacherId, GradeSaveRequest request) {
        Exam exam = examRepository.findById(request.getExamId())
                .orElseThrow(() -> new BusinessException(ErrorCode.EXAM_NOT_FOUND));

        validateSemesterOpen(exam.getAcademicYear(), exam.getSemester());

        SubjectAssignment assignment = subjectAssignmentRepository
                .findValidAssignment(teacherId, classroomId, subjectId, exam.getAcademicYear(), exam.getSemester())
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED, "해당 수업에 대한 성적 입력 권한이 없습니다"));

        request.getScores().forEach(score ->
                validateScore(score.getRawScore(), resolveStatus(score.getAttendanceStatus()), exam));

        List<StudentAffiliation> affiliations = studentAffiliationRepository.findAllByClassroomId(classroomId);
        Map<Long, StudentAffiliation> affiliationByStudentId = affiliations.stream()
                .collect(Collectors.toMap(a -> a.getStudent().getId(), a -> a));

        Set<Long> requestedStudentIds = request.getScores().stream()
                .map(GradeSaveRequest.StudentScoreDto::getStudentId)
                .collect(Collectors.toSet());

        requestedStudentIds.forEach(studentId -> {
            if (!affiliationByStudentId.containsKey(studentId)) {
                throw new BusinessException(ErrorCode.STUDENT_NOT_IN_CLASSROOM,
                        "학급에 속하지 않는 학생입니다. studentId=" + studentId);
            }
        });

        List<Long> studentIds = List.copyOf(requestedStudentIds);
        Map<Long, StudentGrade> existingGradeByStudentId = studentGradeRepository
                .findByExamIdAndSubjectIdAndStudentIds(exam.getId(), subjectId, studentIds)
                .stream()
                .collect(Collectors.toMap(sg -> sg.getStudent().getId(), sg -> sg));

        String teacherName = lookupUserName(teacherId);

        request.getScores().forEach(score -> {
            ExamAttendanceStatus newStatus = resolveStatus(score.getAttendanceStatus());
            Integer normalized = normalize(score.getRawScore(), newStatus);

            StudentGrade existing = existingGradeByStudentId.get(score.getStudentId());
            if (existing != null) {
                Integer beforeScore = existing.getRawScore();
                ExamAttendanceStatus beforeStatus = existing.getAttendanceStatus();
                if (!Objects.equals(beforeScore, normalized) || beforeStatus != newStatus) {
                    existing.update(score.getRawScore(), newStatus);
                    recordHistory(existing, beforeScore, normalized, teacherId, teacherName, null);
                }
            } else {
                Student student = affiliationByStudentId.get(score.getStudentId()).getStudent();
                StudentGrade saved = studentGradeRepository.save(StudentGrade.builder()
                        .student(student)
                        .exam(exam)
                        .subject(assignment.getSubject())
                        .rawScore(score.getRawScore())
                        .attendanceStatus(newStatus)
                        .build());
                recordHistory(saved, null, normalized, teacherId, teacherName, null);
            }
        });

        studentIds.forEach(studentId ->
                semesterStatRecalculator.refresh(affiliationByStudentId.get(studentId).getStudent(),
                        exam.getAcademicYear(), exam.getSemester()));
    }

    // ─── 성적 수정 (과목 담당 교사) ──────────────────────────────────────────

    @Transactional
    public void updateGrade(Long classroomId, Long subjectId, Long gradeId, Long teacherId, GradeUpdateRequest request) {
        StudentGrade grade = studentGradeRepository.findById(gradeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GRADE_NOT_FOUND));

        validateSemesterOpen(grade.getExam().getAcademicYear(), grade.getExam().getSemester());

        if (!grade.getSubject().getId().equals(subjectId)) {
            throw new BusinessException(ErrorCode.GRADE_SUBJECT_MISMATCH);
        }

        studentAffiliationRepository.findByStudentIdAndClassroomId(grade.getStudent().getId(), classroomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_IN_CLASSROOM,
                        "해당 학생은 이 학급에 속하지 않습니다"));

        subjectAssignmentRepository.findValidAssignment(
                        teacherId, classroomId, subjectId,
                        grade.getExam().getAcademicYear(), grade.getExam().getSemester())
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED, "해당 수업에 대한 성적 수정 권한이 없습니다"));

        ExamAttendanceStatus newStatus = resolveStatus(request.getAttendanceStatus());
        Integer normalized = normalize(request.getRawScore(), newStatus);
        validateScore(request.getRawScore(), newStatus, grade.getExam());

        Integer beforeScore = grade.getRawScore();
        ExamAttendanceStatus beforeStatus = grade.getAttendanceStatus();
        if (!Objects.equals(beforeScore, normalized) || beforeStatus != newStatus) {
            grade.update(request.getRawScore(), newStatus);
            recordHistory(grade, beforeScore, normalized,
                    teacherId, lookupUserName(teacherId), request.getReason());
            semesterStatRecalculator.refresh(grade.getStudent(),
                    grade.getExam().getAcademicYear(), grade.getExam().getSemester());
        }
    }

    // ─── 과목별 성적 조회 (과목 담당 교사) ───────────────────────────────────

    public GradeListResponse getSubjectGrades(Long classroomId, Long subjectId, Long teacherId, Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXAM_NOT_FOUND));

        SubjectAssignment assignment = subjectAssignmentRepository
                .findValidAssignment(teacherId, classroomId, subjectId, exam.getAcademicYear(), exam.getSemester())
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED, "해당 수업의 성적 조회 권한이 없습니다"));

        List<StudentAffiliation> affiliations = studentAffiliationRepository.findAllByClassroomId(classroomId);
        List<Long> studentIds = affiliations.stream().map(a -> a.getStudent().getId()).toList();

        Map<Long, StudentGrade> gradeByStudentId = studentGradeRepository
                .findByExamIdAndSubjectIdAndStudentIds(exam.getId(), subjectId, studentIds)
                .stream()
                .collect(Collectors.toMap(sg -> sg.getStudent().getId(), sg -> sg));

        List<GradeListResponse.StudentGradeDto> gradeDtos = affiliations.stream()
                .map(affiliation -> {
                    Student student = affiliation.getStudent();
                    StudentGrade grade = gradeByStudentId.get(student.getId());
                    return GradeListResponse.StudentGradeDto.builder()
                            .gradeId(grade != null ? grade.getId() : null)
                            .studentId(student.getId())
                            .studentName(student.getUser().getName())
                            .studentNum(affiliation.getStudentNum())
                            .rawScore(grade != null ? grade.getRawScore() : null)
                            .attendanceStatus(grade != null ? grade.getAttendanceStatus() : null)
                            .build();
                })
                .toList();

        return GradeListResponse.builder()
                .examId(exam.getId())
                .academicYear(exam.getAcademicYear())
                .semester(exam.getSemester())
                .examType(exam.getExamType())
                .examName(exam.getName())
                .examDate(exam.getExamDate())
                .maxScore(exam.getMaxScore())
                .published(exam.isPublished())
                .subjectName(assignment.getSubject().getName())
                .grades(gradeDtos)
                .build();
    }

    // ─── 전체 성적 조회 (담임 교사) ──────────────────────────────────────────

    public ClassroomGradeResponse getClassroomGrades(Long classroomId, Long teacherId, Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXAM_NOT_FOUND));

        classRoomRepository.findClassroomByHomeroomTeacherIdAndAcademicYearAndSemester(
                        teacherId, exam.getAcademicYear(), exam.getSemester())
                .filter(c -> c.getId().equals(classroomId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED, "담임 교사만 전체 성적을 조회할 수 있습니다"));

        List<StudentAffiliation> affiliations = studentAffiliationRepository.findAllByClassroomId(classroomId);
        List<Long> studentIds = affiliations.stream().map(a -> a.getStudent().getId()).toList();

        Map<Long, List<StudentGrade>> gradesByStudentId = studentGradeRepository
                .findByExamIdAndStudentIds(exam.getId(), studentIds)
                .stream()
                .collect(Collectors.groupingBy(sg -> sg.getStudent().getId()));

        Map<Long, StudentSemesterStat> statByStudentId = semesterStatRepository
                .findByStudentIdsAndYearAndSemester(studentIds, exam.getAcademicYear(), exam.getSemester())
                .stream()
                .collect(Collectors.toMap(s -> s.getStudent().getId(), s -> s));

        List<ClassroomGradeResponse.StudentAllGradesDto> studentDtos = affiliations.stream()
                .map(affiliation -> {
                    Student student = affiliation.getStudent();
                    List<StudentGrade> grades = gradesByStudentId.getOrDefault(student.getId(), List.of());
                    StudentSemesterStat stat = statByStudentId.get(student.getId());

                    List<ClassroomGradeResponse.SubjectScoreDto> subjectScores = grades.stream()
                            .map(g -> ClassroomGradeResponse.SubjectScoreDto.builder()
                                    .gradeId(g.getId())
                                    .subjectName(g.getSubject().getName())
                                    .rawScore(g.getRawScore())
                                    .attendanceStatus(g.getAttendanceStatus())
                                    .build())
                            .toList();

                    return ClassroomGradeResponse.StudentAllGradesDto.builder()
                            .studentId(student.getId())
                            .studentName(student.getUser().getName())
                            .studentNum(affiliation.getStudentNum())
                            .totalScore(stat != null ? stat.getTotalScore() : 0.0)
                            .averageScore(stat != null ? stat.getAverageScore() : 0.0)
                            .gradeLevel(stat != null ? stat.getGradeLevel() : null)
                            .subjectScores(subjectScores)
                            .build();
                })
                .toList();

        return ClassroomGradeResponse.builder()
                .examId(exam.getId())
                .academicYear(exam.getAcademicYear())
                .semester(exam.getSemester())
                .examType(exam.getExamType())
                .examName(exam.getName())
                .examDate(exam.getExamDate())
                .maxScore(exam.getMaxScore())
                .students(studentDtos)
                .build();
    }

    // ─── 헬퍼 ────────────────────────────────────────────────────────────────

    private void validateSemesterOpen(Integer year, Integer semester) {
        if (semesterClosureService.isClosed(year, semester)) {
            throw new BusinessException(ErrorCode.SEMESTER_CLOSED,
                    year + "학년도 " + semester + "학기는 마감되어 수정할 수 없습니다");
        }
    }

    private static ExamAttendanceStatus resolveStatus(ExamAttendanceStatus status) {
        return status != null ? status : ExamAttendanceStatus.PRESENT;
    }

    private static Integer normalize(Integer rawScore, ExamAttendanceStatus status) {
        return switch (status) {
            case ABSENT -> null;
            case CHEATED, NOT_SUBMITTED -> 0;
            case PRESENT -> rawScore;
        };
    }

    private void validateScore(Integer rawScore, ExamAttendanceStatus status, Exam exam) {
        if (status != ExamAttendanceStatus.PRESENT) return;
        if (rawScore == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "응시 학생은 점수가 필수입니다");
        }
        if (rawScore < 0 || rawScore > exam.getMaxScore()) {
            throw new BusinessException(ErrorCode.EXAM_SCORE_OUT_OF_RANGE,
                    "점수는 0 ~ " + exam.getMaxScore() + " 범위여야 합니다");
        }
    }

    private void recordHistory(StudentGrade grade, Integer before, Integer after,
                               Long changedByUserId, String changedByName, String reason) {
        gradeHistoryRepository.save(GradeHistory.builder()
                .studentGrade(grade)
                .beforeScore(before)
                .afterScore(after)
                .changedByUserId(changedByUserId)
                .changedByName(changedByName)
                .reason(reason)
                .build());
    }

    private String lookupUserName(Long userId) {
        return userRepository.findById(userId).map(User::getName).orElse("(unknown)");
    }
}
