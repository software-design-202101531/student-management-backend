package com.school.studentmanagement.grade.service;

import com.school.studentmanagement.StudentAffiliation.entity.StudentAffiliation;
import com.school.studentmanagement.StudentAffiliation.repository.StudentAffiliationRepository;
import com.school.studentmanagement.classroom.repository.ClassRoomRepository;
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
import com.school.studentmanagement.user.entity.Student;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
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
        // 1. 해당 교사가 이 학급·과목을 담당하는지 검증
        SubjectAssignment assignment = subjectAssignmentRepository
                .findValidAssignment(teacherId, classroomId, subjectId, request.getAcademicYear(), request.getSemester())
                .orElseThrow(() -> new AccessDeniedException("해당 수업에 대한 성적 입력 권한이 없습니다."));

        // 2. 시험 정보 조회 또는 생성
        Exam exam = examRepository
                .findByAcademicYearAndSemesterAndExamType(request.getAcademicYear(), request.getSemester(), request.getExamType())
                .orElseGet(() -> examRepository.save(Exam.builder()
                        .academicYear(request.getAcademicYear())
                        .semester(request.getSemester())
                        .examType(request.getExamType())
                        .build()));

        // 3. 학급 소속 학생 목록 로드 및 학생 ID 검증
        List<StudentAffiliation> affiliations = studentAffiliationRepository.findAllByClassroomId(classroomId);
        Map<Long, StudentAffiliation> affiliationByStudentId = affiliations.stream()
                .collect(Collectors.toMap(a -> a.getStudent().getId(), a -> a));

        Set<Long> requestedStudentIds = request.getScores().stream()
                .map(GradeSaveRequest.StudentScoreDto::getStudentId)
                .collect(Collectors.toSet());

        requestedStudentIds.forEach(studentId -> {
            if (!affiliationByStudentId.containsKey(studentId)) {
                throw new IllegalArgumentException("학급에 속하지 않는 학생입니다. studentId=" + studentId);
            }
        });

        // 4. 기존 성적 맵 구성 (studentId → StudentGrade)
        List<Long> studentIds = List.copyOf(requestedStudentIds);
        Map<Long, StudentGrade> existingGradeByStudentId = studentGradeRepository
                .findByExamIdAndSubjectIdAndStudentIds(exam.getId(), subjectId, studentIds)
                .stream()
                .collect(Collectors.toMap(sg -> sg.getStudent().getId(), sg -> sg));

        // 5. 성적 저장 또는 수정
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

        // 6. 학기 통계 갱신
        studentIds.forEach(studentId ->
                refreshSemesterStat(affiliationByStudentId.get(studentId).getStudent(),
                        request.getAcademicYear(), request.getSemester()));
    }

    // ─── 성적 수정 (과목 담당 교사) ──────────────────────────────────────────

    @Transactional
    public void updateGrade(Long classroomId, Long subjectId, Long gradeId, Long teacherId, GradeUpdateRequest request) {
        // 1. 성적 존재 여부 확인
        StudentGrade grade = studentGradeRepository.findById(gradeId)
                .orElseThrow(() -> new IllegalArgumentException("성적 정보를 찾을 수 없습니다."));

        // 2. 과목 일치 여부 확인
        if (!grade.getSubject().getId().equals(subjectId)) {
            throw new IllegalArgumentException("성적 과목 정보가 일치하지 않습니다.");
        }

        // 3. 해당 학생이 이 학급 소속인지 + 교사가 담당자인지 검증
        studentAffiliationRepository.findByStudentIdAndClassroomId(grade.getStudent().getId(), classroomId)
                .orElseThrow(() -> new IllegalArgumentException("해당 학생은 이 학급에 속하지 않습니다."));

        subjectAssignmentRepository.findValidAssignment(
                        teacherId, classroomId, subjectId,
                        grade.getExam().getAcademicYear(), grade.getExam().getSemester())
                .orElseThrow(() -> new AccessDeniedException("해당 수업에 대한 성적 수정 권한이 없습니다."));

        // 4. 점수 수정
        grade.updateScore(request.getRawScore());

        // 5. 학기 통계 갱신
        refreshSemesterStat(grade.getStudent(), grade.getExam().getAcademicYear(), grade.getExam().getSemester());
    }

    // ─── 과목별 성적 조회 (과목 담당 교사) ───────────────────────────────────

    public GradeListResponse getSubjectGrades(Long classroomId, Long subjectId, Long teacherId,
                                              Integer academicYear, Integer semester, ExamType examType) {
        // 1. 담당 교사 권한 검증
        SubjectAssignment assignment = subjectAssignmentRepository
                .findValidAssignment(teacherId, classroomId, subjectId, academicYear, semester)
                .orElseThrow(() -> new AccessDeniedException("해당 수업의 성적 조회 권한이 없습니다."));

        // 2. 시험 정보 조회
        Exam exam = examRepository.findByAcademicYearAndSemesterAndExamType(academicYear, semester, examType)
                .orElseThrow(() -> new IllegalArgumentException("해당 시험 정보를 찾을 수 없습니다."));

        // 3. 학급 학생 목록 조회
        List<StudentAffiliation> affiliations = studentAffiliationRepository.findAllByClassroomId(classroomId);
        List<Long> studentIds = affiliations.stream().map(a -> a.getStudent().getId()).toList();

        // 4. 해당 시험·과목의 성적 맵 구성
        Map<Long, StudentGrade> gradeByStudentId = studentGradeRepository
                .findByExamIdAndSubjectIdAndStudentIds(exam.getId(), subjectId, studentIds)
                .stream()
                .collect(Collectors.toMap(sg -> sg.getStudent().getId(), sg -> sg));

        // 5. 응답 빌드 (미입력 학생은 gradeId, rawScore = null)
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
        // 1. 담임 교사 권한 검증
        classRoomRepository.findClassroomByHomeroomTeacherIdAndAcademicYearAndSemester(teacherId, academicYear, semester)
                .filter(c -> c.getId().equals(classroomId))
                .orElseThrow(() -> new AccessDeniedException("담임 교사만 전체 성적을 조회할 수 있습니다."));

        // 2. 시험 정보 조회
        Exam exam = examRepository.findByAcademicYearAndSemesterAndExamType(academicYear, semester, examType)
                .orElseThrow(() -> new IllegalArgumentException("해당 시험 정보를 찾을 수 없습니다."));

        // 3. 학급 학생 목록 조회
        List<StudentAffiliation> affiliations = studentAffiliationRepository.findAllByClassroomId(classroomId);
        List<Long> studentIds = affiliations.stream().map(a -> a.getStudent().getId()).toList();

        // 4. 해당 시험의 전 과목 성적 (학생별 그룹핑)
        Map<Long, List<StudentGrade>> gradesByStudentId = studentGradeRepository
                .findByExamIdAndStudentIds(exam.getId(), studentIds)
                .stream()
                .collect(Collectors.groupingBy(sg -> sg.getStudent().getId()));

        // 5. 학기 누적 통계 맵 구성
        Map<Long, StudentSemesterStat> statByStudentId = semesterStatRepository
                .findByStudentIdsAndYearAndSemester(studentIds, academicYear, semester)
                .stream()
                .collect(Collectors.toMap(s -> s.getStudent().getId(), s -> s));

        // 6. 응답 빌드
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
