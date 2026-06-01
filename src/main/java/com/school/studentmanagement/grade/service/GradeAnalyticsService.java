package com.school.studentmanagement.grade.service;

import com.school.studentmanagement.classroom.entity.Classroom;
import com.school.studentmanagement.classroom.entity.StudentAffiliation;
import com.school.studentmanagement.classroom.repository.StudentAffiliationRepository;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.exception.ErrorCode;
import com.school.studentmanagement.analytics.repository.AnalyticsGradeQueryRepository;
import com.school.studentmanagement.global.util.AcademicCalendarUtil;
import com.school.studentmanagement.grade.dto.GradeTrendResponse;
import com.school.studentmanagement.grade.dto.RadarChartResponse;
import com.school.studentmanagement.grade.dto.StudentOverviewResponse;
import com.school.studentmanagement.grade.entity.Exam;
import com.school.studentmanagement.grade.entity.StudentGrade;
import com.school.studentmanagement.grade.entity.StudentSemesterStat;
import com.school.studentmanagement.grade.repository.ClassSubjectScoreAggregation;
import com.school.studentmanagement.grade.repository.StudentGradeRepository;
import com.school.studentmanagement.grade.repository.StudentSemesterStatRepository;
import com.school.studentmanagement.grade.repository.SubjectScoreAggregation;
import com.school.studentmanagement.parent.validator.ParentChildLinkValidator;
import com.school.studentmanagement.student.entity.Student;
import com.school.studentmanagement.student.repository.StudentRepository;
import com.school.studentmanagement.subject.entity.Subject;
import com.school.studentmanagement.subject.repository.SubjectAssignmentRepository;
import com.school.studentmanagement.subject.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GradeAnalyticsService {

    private final StudentGradeRepository studentGradeRepository;
    private final StudentSemesterStatRepository semesterStatRepository;
    private final StudentAffiliationRepository studentAffiliationRepository;
    private final SubjectRepository subjectRepository;
    private final SubjectAssignmentRepository subjectAssignmentRepository;
    private final ParentChildLinkValidator parentChildLinkValidator;
    private final StudentRepository studentRepository;
    private final AcademicCalendarUtil academicCalendarUtil;
    private final AnalyticsGradeQueryRepository analyticsGradeQueryRepository;

    // ─── 레이더 차트 ─────────────────────────────────────────────────────────

    public RadarChartResponse getStudentRadar(Long studentId, Integer academicYear, Integer semester) {
        int year = academicYear != null ? academicYear : academicCalendarUtil.getCurrentAcademicYear();
        int sem = semester != null ? semester : academicCalendarUtil.getCurrentSemester();
        return buildRadar(studentId, year, sem);
    }

    public RadarChartResponse getChildRadar(Long parentId, Long studentId, Integer academicYear, Integer semester) {
        parentChildLinkValidator.validateLinked(parentId, studentId);
        return getStudentRadar(studentId, academicYear, semester);
    }

    public RadarChartResponse getRadarForTeacher(Long teacherId, Long studentId, Integer academicYear, Integer semester) {
        int year = academicYear != null ? academicYear : academicCalendarUtil.getCurrentAcademicYear();
        int sem = semester != null ? semester : academicCalendarUtil.getCurrentSemester();
        validateTeacherCanViewStudent(teacherId, studentId, year, sem);
        return buildRadar(studentId, year, sem);
    }

    private RadarChartResponse buildRadar(Long studentId, int year, int semester) {
        StudentAffiliation aff = studentAffiliationRepository.findWithAllDetails(studentId, year, semester)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_ASSIGNED));

        // 학생 본인의 과목별 학기점수 — analytics 사전 집계에서 조회(운영 OLTP 직접 집계 이관, P3)
        Map<Long, Double> studentScores = analyticsGradeQueryRepository
                .findStudentSubjectScores(studentId, year, semester);

        // 학급 전체의 과목별 평균 — analytics에서 학급 학생들의 가중점수 평균을 집계
        List<StudentAffiliation> classAffiliations = studentAffiliationRepository
                .findAllByClassroomId(aff.getClassroom().getId());
        List<Long> classStudentIds = classAffiliations.stream()
                .map(a -> a.getStudent().getId())
                .toList();

        Map<Long, Double> classAverages = analyticsGradeQueryRepository
                .findClassSubjectAverages(classStudentIds, year, semester);

        // 과목명: 본인 점수 또는 학급 평균이 존재하는 과목들의 합집합
        Set<Long> subjectIds = new LinkedHashSet<>();
        subjectIds.addAll(studentScores.keySet());
        subjectIds.addAll(classAverages.keySet());
        Map<Long, String> subjectNames = subjectRepository.findAllById(subjectIds).stream()
                .collect(Collectors.toMap(Subject::getId, Subject::getName));

        List<RadarChartResponse.SubjectRadarDto> subjectDtos = subjectIds.stream()
                .map(id -> RadarChartResponse.SubjectRadarDto.builder()
                        .subjectId(id)
                        .subjectName(subjectNames.getOrDefault(id, "(unknown)"))
                        .studentScore(studentScores.get(id))
                        .classAverage(classAverages.get(id))
                        .build())
                .toList();

        return RadarChartResponse.builder()
                .studentId(studentId)
                .studentName(aff.getStudent().getUser().getName())
                .academicYear(year)
                .semester(semester)
                .subjects(subjectDtos)
                .build();
    }

    // ─── 학생 종합 뷰 (교사용) ───────────────────────────────────────────────

    public StudentOverviewResponse getStudentOverviewForTeacher(Long teacherId, Long studentId,
                                                                Integer academicYear, Integer semester) {
        int year = academicYear != null ? academicYear : academicCalendarUtil.getCurrentAcademicYear();
        int sem = semester != null ? semester : academicCalendarUtil.getCurrentSemester();
        validateTeacherCanViewStudent(teacherId, studentId, year, sem);

        StudentAffiliation aff = studentAffiliationRepository.findWithAllDetails(studentId, year, sem)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_ASSIGNED));
        Classroom classroom = aff.getClassroom();

        // 학기 누적 통계
        StudentSemesterStat stat = semesterStatRepository
                .findByStudentIdAndAcademicYearAndSemester(studentId, year, sem)
                .orElse(null);

        // 본인 과목별 학기점수
        Map<Long, Double> studentScores = studentGradeRepository
                .aggregateSubjectScoresByStudentAndSemester(studentId, year, sem)
                .stream()
                .collect(Collectors.toMap(SubjectScoreAggregation::getSubjectId,
                        SubjectScoreAggregation::getSubjectScore));

        // 학급 평균 + 학급 학생들의 학기 통계 (석차)
        List<StudentAffiliation> classAffiliations = studentAffiliationRepository
                .findAllByClassroomId(classroom.getId());
        List<Long> classStudentIds = classAffiliations.stream()
                .map(a -> a.getStudent().getId())
                .toList();

        Map<Long, List<Double>> scoresBySubject = studentGradeRepository
                .aggregateSubjectScoresByStudentIdsAndSemester(classStudentIds, year, sem)
                .stream()
                .collect(Collectors.groupingBy(
                        ClassSubjectScoreAggregation::getSubjectId,
                        Collectors.mapping(ClassSubjectScoreAggregation::getSubjectScore, Collectors.toList())
                ));

        Set<Long> subjectIds = new LinkedHashSet<>();
        subjectIds.addAll(studentScores.keySet());
        subjectIds.addAll(scoresBySubject.keySet());
        Map<Long, String> subjectNames = subjectRepository.findAllById(subjectIds).stream()
                .collect(Collectors.toMap(Subject::getId, Subject::getName));

        List<StudentOverviewResponse.SubjectSemesterScoreDto> subjectScoreDtos = subjectIds.stream()
                .map(id -> {
                    List<Double> classList = scoresBySubject.getOrDefault(id, List.of());
                    Double classAvg = classList.isEmpty() ? null
                            : classList.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                    return StudentOverviewResponse.SubjectSemesterScoreDto.builder()
                            .subjectId(id)
                            .subjectName(subjectNames.getOrDefault(id, "(unknown)"))
                            .semesterScore(studentScores.get(id))
                            .classAverage(classAvg)
                            .build();
                })
                .toList();

        // 시험별 결과 (published 무관, 교사용)
        List<StudentGrade> grades = studentGradeRepository
                .findByStudentIdAndAcademicYearAndSemester(studentId, year, sem);

        Map<Long, List<StudentGrade>> byExamId = new LinkedHashMap<>();
        for (StudentGrade g : grades) {
            byExamId.computeIfAbsent(g.getExam().getId(), k -> new ArrayList<>()).add(g);
        }

        List<StudentOverviewResponse.ExamResultDto> examResults = byExamId.values().stream()
                .map(list -> {
                    Exam exam = list.get(0).getExam();
                    List<StudentOverviewResponse.SubjectScoreDto> subjects = list.stream()
                            .map(g -> StudentOverviewResponse.SubjectScoreDto.builder()
                                    .gradeId(g.getId())
                                    .subjectName(g.getSubject().getName())
                                    .rawScore(g.getRawScore())
                                    .attendanceStatus(g.getAttendanceStatus())
                                    .build())
                            .toList();
                    return StudentOverviewResponse.ExamResultDto.builder()
                            .examId(exam.getId())
                            .examType(exam.getExamType())
                            .examName(exam.getName())
                            .examDate(exam.getExamDate())
                            .maxScore(exam.getMaxScore())
                            .coverage(exam.getCoverage())
                            .published(exam.isPublished())
                            .subjects(subjects)
                            .build();
                })
                .toList();

        // 학급 내 석차 (RANK semantics)
        List<StudentSemesterStat> classStats = semesterStatRepository
                .findByStudentIdsAndYearAndSemester(classStudentIds, year, sem);
        Integer classRank = computeRank(classStats, studentId);

        return StudentOverviewResponse.builder()
                .studentId(studentId)
                .studentName(aff.getStudent().getUser().getName())
                .grade(classroom.getGrade())
                .classNum(classroom.getClassNum())
                .studentNum(aff.getStudentNum())
                .academicYear(year)
                .semester(sem)
                .totalScore(stat != null ? stat.getTotalScore() : 0.0)
                .averageScore(stat != null ? stat.getAverageScore() : 0.0)
                .gradeLevel(stat != null ? stat.getGradeLevel() : null)
                .classRank(classRank)
                .classSize(classAffiliations.size())
                .subjectScores(subjectScoreDtos)
                .examResults(examResults)
                .build();
    }

    // ─── 시계열 추이 ──────────────────────────────────────────────────────────

    public GradeTrendResponse getStudentTrend(Long studentId, Integer fromYear, Integer fromSemester,
                                              Integer toYear, Integer toSemester) {
        return buildTrend(studentId, fromYear, fromSemester, toYear, toSemester);
    }

    public GradeTrendResponse getChildTrend(Long parentId, Long studentId, Integer fromYear, Integer fromSemester,
                                            Integer toYear, Integer toSemester) {
        parentChildLinkValidator.validateLinked(parentId, studentId);
        return buildTrend(studentId, fromYear, fromSemester, toYear, toSemester);
    }

    public GradeTrendResponse getTrendForTeacher(Long teacherId, Long studentId,
                                                 Integer fromYear, Integer fromSemester,
                                                 Integer toYear, Integer toSemester) {
        GradeTrendResponse trend = buildTrend(studentId, fromYear, fromSemester, toYear, toSemester);

        // 구간별 권한 검증: 추이에 실제로 노출되는 각 학기마다 교사-학생 관계를 확인한다.
        // (현재 학기 관계만으로 과거 전체 이력을 열람하던 문제 방지)
        Set<Integer> validated = new HashSet<>();
        for (GradeTrendResponse.SubjectTrendDto subject : trend.getSubjects()) {
            for (GradeTrendResponse.TrendPoint point : subject.getPoints()) {
                int key = point.getAcademicYear() * 10 + point.getSemester();
                if (validated.add(key)) {
                    validateTeacherCanViewStudent(teacherId, studentId,
                            point.getAcademicYear(), point.getSemester());
                }
            }
        }
        // 노출 데이터가 전혀 없으면 최소한 현재 학기 관계를 확인 (임의 학생 probe 방지)
        if (validated.isEmpty()) {
            validateTeacherCanViewStudent(teacherId, studentId,
                    academicCalendarUtil.getCurrentAcademicYear(), academicCalendarUtil.getCurrentSemester());
        }
        return trend;
    }

    private GradeTrendResponse buildTrend(Long studentId, Integer fromYear, Integer fromSemester,
                                          Integer toYear, Integer toSemester) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));

        int fy = fromYear != null ? fromYear : student.getEnrollmentYear();
        int fs = fromSemester != null ? fromSemester : 1;
        int ty = toYear != null ? toYear : academicCalendarUtil.getCurrentAcademicYear();
        int ts = toSemester != null ? toSemester : academicCalendarUtil.getCurrentSemester();

        int fromKey = fy * 10 + fs;
        int toKey = ty * 10 + ts;

        // 추세 점수도 analytics 사전 집계에서 조회(운영 OLTP 직접 집계 이관, P3)
        List<AnalyticsGradeQueryRepository.TrendRow> rows = analyticsGradeQueryRepository
                .findStudentSubjectTrend(studentId, fromKey, toKey);

        Map<Long, List<AnalyticsGradeQueryRepository.TrendRow>> bySubject = rows.stream()
                .collect(Collectors.groupingBy(AnalyticsGradeQueryRepository.TrendRow::subjectId,
                        LinkedHashMap::new, Collectors.toList()));

        Map<Long, String> subjectNames = subjectRepository.findAllById(bySubject.keySet()).stream()
                .collect(Collectors.toMap(Subject::getId, Subject::getName));

        List<GradeTrendResponse.SubjectTrendDto> subjectDtos = bySubject.entrySet().stream()
                .map(e -> GradeTrendResponse.SubjectTrendDto.builder()
                        .subjectId(e.getKey())
                        .subjectName(subjectNames.getOrDefault(e.getKey(), "(unknown)"))
                        .points(e.getValue().stream()
                                .map(p -> GradeTrendResponse.TrendPoint.builder()
                                        .academicYear(p.academicYear())
                                        .semester(p.semester())
                                        .semesterScore(p.score())
                                        .build())
                                .toList())
                        .build())
                .toList();

        return GradeTrendResponse.builder()
                .studentId(studentId)
                .fromYear(fy).fromSemester(fs).toYear(ty).toSemester(ts)
                .subjects(subjectDtos)
                .build();
    }

    // ─── 권한 검증 헬퍼 ──────────────────────────────────────────────────────


    private void validateTeacherCanViewStudent(Long teacherId, Long studentId, int year, int semester) {
        StudentAffiliation aff = studentAffiliationRepository.findWithAllDetails(studentId, year, semester)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_ASSIGNED));

        Long classroomId = aff.getClassroom().getId();
        boolean isHomeroom = aff.getClassroom().getHomeroomTeacher() != null
                && aff.getClassroom().getHomeroomTeacher().getId().equals(teacherId);
        if (isHomeroom) return;

        boolean hasAssignment = subjectAssignmentRepository
                .existsByTeacherIdAndClassroomIdAndAcademicYearAndSemester(teacherId, classroomId, year, semester);
        if (!hasAssignment) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "해당 학생을 조회할 권한이 없습니다");
        }
    }

    // 학기 평균 내림차순 RANK semantics. studentId의 등수 반환 (없으면 null)
    static Integer computeRank(List<StudentSemesterStat> stats, Long studentId) {
        if (stats.isEmpty()) return null;

        List<StudentSemesterStat> sorted = stats.stream()
                .sorted(Comparator.comparing(StudentSemesterStat::getAverageScore).reversed())
                .toList();

        int rank = 0;
        Double prev = null;
        int position = 0;
        for (StudentSemesterStat s : sorted) {
            position++;
            if (prev == null || !prev.equals(s.getAverageScore())) {
                rank = position;
                prev = s.getAverageScore();
            }
            if (s.getStudent().getId().equals(studentId)) {
                return rank;
            }
        }
        return null;
    }
}
