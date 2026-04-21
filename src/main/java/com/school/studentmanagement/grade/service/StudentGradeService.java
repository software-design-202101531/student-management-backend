package com.school.studentmanagement.grade.service;

import com.school.studentmanagement.classroom.entity.StudentAffiliation;
import com.school.studentmanagement.classroom.repository.StudentAffiliationRepository;
import com.school.studentmanagement.classroom.repository.ClassRoomRepository;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.exception.ErrorCode;
import com.school.studentmanagement.grade.dto.ClassroomGradeResponse;
import com.school.studentmanagement.grade.dto.GradeListResponse;
import com.school.studentmanagement.grade.dto.GradeSaveRequest;
import com.school.studentmanagement.grade.dto.GradeUpdateRequest;
import com.school.studentmanagement.grade.entity.Exam;
import com.school.studentmanagement.grade.entity.StudentGrade;
import com.school.studentmanagement.grade.entity.StudentSemesterStat;
import com.school.studentmanagement.grade.repository.ExamRepository;
import com.school.studentmanagement.grade.repository.StudentGradeRepository;
import com.school.studentmanagement.grade.repository.StudentSemesterStatRepository;
import com.school.studentmanagement.global.enums.ExamType;
import com.school.studentmanagement.subject.entity.SubjectAssignment;
import com.school.studentmanagement.subject.repository.SubjectAssignmentRepository;
import com.school.studentmanagement.student.entity.Student;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
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

    // ─── 성적 입력 (과목 담당 교사) ──────────────────────────────────────────

    @Transactional
    public void saveGrades(Long classroomId, Long subjectId, Long teacherId, GradeSaveRequest request) {
        SubjectAssignment assignment = subjectAssignmentRepository
                .findValidAssignment(teacherId, classroomId, subjectId, request.getAcademicYear(), request.getSemester())
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED, "해당 수업에 대한 성적 입력 권한이 없습니다"));

        Exam exam = examRepository
                .findByAcademicYearAndSemesterAndExamType(request.getAcademicYear(), request.getSemester(), request.getExamType())
                .orElseGet(() -> examRepository.save(Exam.builder()
                        .academicYear(request.getAcademicYear())
                        .semester(request.getSemester())
                        .examType(request.getExamType())
                        .build()));

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

        request.getScores().forEach(score -> {
            StudentGrade existing = existingGradeByStudentId.get(score.getStudentId());
            if (existing != null) {
                existing.updateScore(score.getRawScore());
            } else {
                Student student = affiliationByStudentId.get(score.getStudentId()).getStudent();
                studentGradeRepository.save(StudentGrade.builder()
                        .student(student)
                        .exam(exam)
                        .subject(assignment.getSubject())
                        .rawScore(score.getRawScore())
                        .build());
            }
        });

        studentIds.forEach(studentId ->
                refreshSemesterStat(affiliationByStudentId.get(studentId).getStudent(),
                        request.getAcademicYear(), request.getSemester()));
    }

    // ─── 성적 수정 (과목 담당 교사) ──────────────────────────────────────────

    @Transactional
    public void updateGrade(Long classroomId, Long subjectId, Long gradeId, Long teacherId, GradeUpdateRequest request) {
        StudentGrade grade = studentGradeRepository.findById(gradeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GRADE_NOT_FOUND));

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

        grade.updateScore(request.getRawScore());

        refreshSemesterStat(grade.getStudent(), grade.getExam().getAcademicYear(), grade.getExam().getSemester());
    }

    // ─── 과목별 성적 조회 (과목 담당 교사) ───────────────────────────────────

    public GradeListResponse getSubjectGrades(Long classroomId, Long subjectId, Long teacherId,
                                              Integer academicYear, Integer semester, ExamType examType) {
        SubjectAssignment assignment = subjectAssignmentRepository
                .findValidAssignment(teacherId, classroomId, subjectId, academicYear, semester)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED, "해당 수업의 성적 조회 권한이 없습니다"));

        Exam exam = examRepository.findByAcademicYearAndSemesterAndExamType(academicYear, semester, examType)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXAM_NOT_FOUND));

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
                            .build();
                })
                .toList();

        return GradeListResponse.builder()
                .academicYear(academicYear)
                .semester(semester)
                .examType(examType)
                .subjectName(assignment.getSubject().getName())
                .grades(gradeDtos)
                .build();
    }

    // ─── 전체 성적 조회 (담임 교사) ──────────────────────────────────────────

    public ClassroomGradeResponse getClassroomGrades(Long classroomId, Long teacherId,
                                                     Integer academicYear, Integer semester, ExamType examType) {
        classRoomRepository.findClassroomByHomeroomTeacherIdAndAcademicYearAndSemester(teacherId, academicYear, semester)
                .filter(c -> c.getId().equals(classroomId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED, "담임 교사만 전체 성적을 조회할 수 있습니다"));

        Exam exam = examRepository.findByAcademicYearAndSemesterAndExamType(academicYear, semester, examType)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXAM_NOT_FOUND));

        List<StudentAffiliation> affiliations = studentAffiliationRepository.findAllByClassroomId(classroomId);
        List<Long> studentIds = affiliations.stream().map(a -> a.getStudent().getId()).toList();

        Map<Long, List<StudentGrade>> gradesByStudentId = studentGradeRepository
                .findByExamIdAndStudentIds(exam.getId(), studentIds)
                .stream()
                .collect(Collectors.groupingBy(sg -> sg.getStudent().getId()));

        Map<Long, StudentSemesterStat> statByStudentId = semesterStatRepository
                .findByStudentIdsAndYearAndSemester(studentIds, academicYear, semester)
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
                                    .build())
                            .toList();

                    return ClassroomGradeResponse.StudentAllGradesDto.builder()
                            .studentId(student.getId())
                            .studentName(student.getUser().getName())
                            .studentNum(affiliation.getStudentNum())
                            .totalScore(stat != null ? stat.getTotalScore() : 0)
                            .averageScore(stat != null ? stat.getAverageScore() : 0.0)
                            .subjectScores(subjectScores)
                            .build();
                })
                .toList();

        return ClassroomGradeResponse.builder()
                .academicYear(academicYear)
                .semester(semester)
                .examType(examType)
                .students(studentDtos)
                .build();
    }

    // ─── 학기 통계 갱신 헬퍼 ─────────────────────────────────────────────────

    private void refreshSemesterStat(Student student, Integer academicYear, Integer semester) {
        Integer total = studentGradeRepository.sumTotalScoreByStudentAndSemester(student.getId(), academicYear, semester);
        Long count = studentGradeRepository.countByStudentAndSemester(student.getId(), academicYear, semester);

        if (count == 0) return;

        double average = total.doubleValue() / count;

        semesterStatRepository.findByStudentIdAndAcademicYearAndSemester(student.getId(), academicYear, semester)
                .ifPresentOrElse(
                        stat -> stat.updateStats(total, average),
                        () -> semesterStatRepository.save(StudentSemesterStat.builder()
                                .student(student)
                                .academicYear(academicYear)
                                .semester(semester)
                                .totalScore(total)
                                .averageScore(average)
                                .build())
                );
    }
}
