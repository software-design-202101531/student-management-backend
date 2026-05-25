package com.school.studentmanagement.grade.service;

import com.school.studentmanagement.classroom.entity.Classroom;
import com.school.studentmanagement.classroom.entity.StudentAffiliation;
import com.school.studentmanagement.classroom.repository.ClassRoomRepository;
import com.school.studentmanagement.classroom.repository.StudentAffiliationRepository;
import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.exception.ErrorCode;
import com.school.studentmanagement.global.util.AcademicCalendarUtil;
import com.school.studentmanagement.grade.dto.ClassroomRankingResponse;
import com.school.studentmanagement.grade.dto.ClassroomStatsResponse;
import com.school.studentmanagement.grade.dto.GradeWideRankingResponse;
import com.school.studentmanagement.grade.dto.StudentGradeWideRankingResponse;
import com.school.studentmanagement.grade.dto.StudentRankingResponse;
import com.school.studentmanagement.grade.entity.Exam;
import com.school.studentmanagement.grade.entity.StudentSemesterStat;
import com.school.studentmanagement.grade.repository.ExamRepository;
import com.school.studentmanagement.grade.repository.StudentGradeRepository;
import com.school.studentmanagement.grade.repository.StudentSemesterStatRepository;
import com.school.studentmanagement.parent.repository.ParentStudentMappingRepository;
import com.school.studentmanagement.subject.entity.Subject;
import com.school.studentmanagement.subject.repository.SubjectAssignmentRepository;
import com.school.studentmanagement.subject.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassroomStatsService {

    private final StudentGradeRepository studentGradeRepository;
    private final StudentSemesterStatRepository semesterStatRepository;
    private final ExamRepository examRepository;
    private final SubjectRepository subjectRepository;
    private final SubjectAssignmentRepository subjectAssignmentRepository;
    private final StudentAffiliationRepository studentAffiliationRepository;
    private final ClassRoomRepository classRoomRepository;
    private final ParentStudentMappingRepository parentMappingRepository;
    private final AcademicCalendarUtil academicCalendarUtil;

    // ─── 학급 통계 ───────────────────────────────────────────────────────────

    public ClassroomStatsResponse getClassroomStats(Long classroomId, Long teacherId, Long examId, Long subjectId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXAM_NOT_FOUND));

        validateTeacherCanViewClassroomSubject(teacherId, classroomId, subjectId,
                exam.getAcademicYear(), exam.getSemester());

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "과목 정보를 찾을 수 없습니다"));

        List<StudentAffiliation> affiliations = studentAffiliationRepository.findAllByClassroomId(classroomId);
        List<Long> studentIds = affiliations.stream().map(a -> a.getStudent().getId()).toList();

        List<Integer> rawScores = studentIds.isEmpty()
                ? List.of()
                : studentGradeRepository.findRawScoresByExamIdAndSubjectIdAndStudentIds(examId, subjectId, studentIds);

        int n = rawScores.size();
        double avg = 0.0, stddev = 0.0;
        Integer max = null, min = null;
        if (n > 0) {
            avg = rawScores.stream().mapToInt(Integer::intValue).average().orElse(0.0);
            double mean = avg;
            stddev = Math.sqrt(rawScores.stream()
                    .mapToDouble(s -> (s - mean) * (s - mean))
                    .average().orElse(0.0));
            max = rawScores.stream().mapToInt(Integer::intValue).max().orElse(0);
            min = rawScores.stream().mapToInt(Integer::intValue).min().orElse(0);
        }

        return ClassroomStatsResponse.builder()
                .classroomId(classroomId)
                .examId(examId)
                .examName(exam.getName())
                .examType(exam.getExamType())
                .subjectId(subjectId)
                .subjectName(subject.getName())
                .maxScore(exam.getMaxScore())
                .studentCount(n)
                .averageScore(avg)
                .standardDeviation(stddev)
                .maxValue(max)
                .minValue(min)
                .distribution(buildDistribution(rawScores, exam.getMaxScore()))
                .build();
    }

    // ─── 학급 석차 ───────────────────────────────────────────────────────────

    public ClassroomRankingResponse getClassroomRanking(Long classroomId, Long teacherId,
                                                       Integer academicYear, Integer semester) {
        int year = academicYear != null ? academicYear : academicCalendarUtil.getCurrentAcademicYear();
        int sem = semester != null ? semester : academicCalendarUtil.getCurrentSemester();

        classRoomRepository.findClassroomByHomeroomTeacherIdAndAcademicYearAndSemester(teacherId, year, sem)
                .filter(c -> c.getId().equals(classroomId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED, "담임 교사만 학급 석차를 조회할 수 있습니다"));

        List<StudentAffiliation> affiliations = studentAffiliationRepository.findAllByClassroomId(classroomId);
        List<Long> studentIds = affiliations.stream().map(a -> a.getStudent().getId()).toList();

        List<StudentSemesterStat> stats = studentIds.isEmpty()
                ? List.of()
                : semesterStatRepository.findByStudentIdsAndYearAndSemester(studentIds, year, sem);

        List<ClassroomRankingResponse.RankingEntry> rankings = computeClassroomRankings(stats, affiliations);

        return ClassroomRankingResponse.builder()
                .classroomId(classroomId)
                .academicYear(year)
                .semester(sem)
                .classSize(affiliations.size())
                .rankings(rankings)
                .build();
    }

    // ─── 학생 본인 학급 석차 ─────────────────────────────────────────────────

    public StudentRankingResponse getMyRanking(Long studentId, Integer academicYear, Integer semester) {
        int year = academicYear != null ? academicYear : academicCalendarUtil.getCurrentAcademicYear();
        int sem = semester != null ? semester : academicCalendarUtil.getCurrentSemester();
        return buildStudentRanking(studentId, year, sem);
    }

    public StudentRankingResponse getChildRanking(Long parentId, Long studentId,
                                                  Integer academicYear, Integer semester) {
        validateParentChild(parentId, studentId);
        return getMyRanking(studentId, academicYear, semester);
    }

    private StudentRankingResponse buildStudentRanking(Long studentId, int year, int sem) {
        StudentAffiliation aff = studentAffiliationRepository.findWithAllDetails(studentId, year, sem)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_ASSIGNED));
        Classroom classroom = aff.getClassroom();

        List<StudentAffiliation> classAffiliations = studentAffiliationRepository.findAllByClassroomId(classroom.getId());
        List<Long> classStudentIds = classAffiliations.stream().map(a -> a.getStudent().getId()).toList();

        List<StudentSemesterStat> stats = semesterStatRepository
                .findByStudentIdsAndYearAndSemester(classStudentIds, year, sem);

        Integer rank = GradeAnalyticsService.computeRank(stats, studentId);

        StudentSemesterStat myStat = stats.stream()
                .filter(s -> s.getStudent().getId().equals(studentId))
                .findFirst().orElse(null);

        return StudentRankingResponse.builder()
                .academicYear(year)
                .semester(sem)
                .rank(rank)
                .classSize(classAffiliations.size())
                .averageScore(myStat != null ? myStat.getAverageScore() : null)
                .gradeLevel(myStat != null ? myStat.getGradeLevel() : null)
                .build();
    }

    // ─── 학년 단위 석차 (교사) ───────────────────────────────────────────────

    public GradeWideRankingResponse getGradeWideRanking(Integer academicYear, Integer semester, Integer grade) {
        int year = academicYear != null ? academicYear : academicCalendarUtil.getCurrentAcademicYear();
        int sem = semester != null ? semester : academicCalendarUtil.getCurrentSemester();

        List<StudentAffiliation> affs = studentAffiliationRepository
                .findAllByYearAndSemesterAndGrade(year, sem, grade);
        List<Long> studentIds = affs.stream().map(a -> a.getStudent().getId()).toList();

        Map<Long, StudentSemesterStat> statByStudentId = studentIds.isEmpty() ? Map.of()
                : semesterStatRepository.findByStudentIdsAndYearAndSemester(studentIds, year, sem)
                    .stream().collect(Collectors.toMap(s -> s.getStudent().getId(), s -> s));

        Map<Long, StudentAffiliation> affByStudentId = affs.stream()
                .collect(Collectors.toMap(a -> a.getStudent().getId(), a -> a));

        List<StudentSemesterStat> sortedStats = statByStudentId.values().stream()
                .sorted(Comparator.comparing(StudentSemesterStat::getAverageScore).reversed())
                .toList();

        List<GradeWideRankingResponse.RankingEntry> rankings = new ArrayList<>();
        int rank = 0;
        Double prev = null;
        int position = 0;
        for (StudentSemesterStat s : sortedStats) {
            position++;
            if (prev == null || !prev.equals(s.getAverageScore())) {
                rank = position;
                prev = s.getAverageScore();
            }
            StudentAffiliation aff = affByStudentId.get(s.getStudent().getId());
            rankings.add(GradeWideRankingResponse.RankingEntry.builder()
                    .rank(rank)
                    .studentId(s.getStudent().getId())
                    .studentName(s.getStudent().getUser().getName())
                    .classNum(aff != null ? aff.getClassroom().getClassNum() : null)
                    .studentNum(aff != null ? aff.getStudentNum() : null)
                    .totalScore(s.getTotalScore())
                    .averageScore(s.getAverageScore())
                    .gradeLevel(s.getGradeLevel())
                    .build());
        }

        return GradeWideRankingResponse.builder()
                .academicYear(year)
                .semester(sem)
                .grade(grade)
                .studentCount(affs.size())
                .rankings(rankings)
                .build();
    }

    // ─── 학년 단위 본인 석차 (학생/학부모) ───────────────────────────────────

    public StudentGradeWideRankingResponse getMyGradeWideRanking(Long studentId, Integer academicYear, Integer semester) {
        int year = academicYear != null ? academicYear : academicCalendarUtil.getCurrentAcademicYear();
        int sem = semester != null ? semester : academicCalendarUtil.getCurrentSemester();

        StudentAffiliation aff = studentAffiliationRepository.findWithAllDetails(studentId, year, sem)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_ASSIGNED));
        Integer grade = aff.getClassroom().getGrade();

        List<StudentAffiliation> gradeAffs = studentAffiliationRepository
                .findAllByYearAndSemesterAndGrade(year, sem, grade);
        List<Long> ids = gradeAffs.stream().map(a -> a.getStudent().getId()).toList();

        List<StudentSemesterStat> stats = ids.isEmpty() ? List.of()
                : semesterStatRepository.findByStudentIdsAndYearAndSemester(ids, year, sem);

        Integer rank = GradeAnalyticsService.computeRank(stats, studentId);
        StudentSemesterStat myStat = stats.stream()
                .filter(s -> s.getStudent().getId().equals(studentId))
                .findFirst().orElse(null);

        return StudentGradeWideRankingResponse.builder()
                .academicYear(year)
                .semester(sem)
                .grade(grade)
                .rank(rank)
                .studentCount(gradeAffs.size())
                .averageScore(myStat != null ? myStat.getAverageScore() : null)
                .gradeLevel(myStat != null ? myStat.getGradeLevel() : null)
                .build();
    }

    public StudentGradeWideRankingResponse getChildGradeWideRanking(Long parentId, Long studentId,
                                                                    Integer academicYear, Integer semester) {
        validateParentChild(parentId, studentId);
        return getMyGradeWideRanking(studentId, academicYear, semester);
    }

    // ─── 헬퍼 ────────────────────────────────────────────────────────────────

    private List<ClassroomRankingResponse.RankingEntry> computeClassroomRankings(
            List<StudentSemesterStat> stats, List<StudentAffiliation> affiliations) {

        List<StudentSemesterStat> sorted = stats.stream()
                .sorted(Comparator.comparing(StudentSemesterStat::getAverageScore).reversed())
                .toList();

        Map<Long, Integer> studentNumByStudentId = new HashMap<>();
        for (StudentAffiliation a : affiliations) {
            studentNumByStudentId.put(a.getStudent().getId(), a.getStudentNum());
        }

        List<ClassroomRankingResponse.RankingEntry> result = new ArrayList<>();
        int rank = 0;
        Double prev = null;
        int position = 0;
        for (StudentSemesterStat s : sorted) {
            position++;
            if (prev == null || !prev.equals(s.getAverageScore())) {
                rank = position;
                prev = s.getAverageScore();
            }
            result.add(ClassroomRankingResponse.RankingEntry.builder()
                    .rank(rank)
                    .studentId(s.getStudent().getId())
                    .studentName(s.getStudent().getUser().getName())
                    .studentNum(studentNumByStudentId.get(s.getStudent().getId()))
                    .totalScore(s.getTotalScore())
                    .averageScore(s.getAverageScore())
                    .gradeLevel(s.getGradeLevel())
                    .build());
        }
        return result;
    }

    private List<ClassroomStatsResponse.ScoreBin> buildDistribution(List<Integer> rawScores, int maxScore) {
        int[] bins = new int[10];
        for (Integer s : rawScores) {
            double normalized = s * 100.0 / maxScore;
            int idx = (int) Math.min(normalized / 10.0, 9);
            bins[idx]++;
        }

        List<ClassroomStatsResponse.ScoreBin> result = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String range = (i < 9) ? (i * 10) + "-" + (i * 10 + 9) : "90-100";
            result.add(ClassroomStatsResponse.ScoreBin.builder()
                    .range(range).count(bins[i]).build());
        }
        return result;
    }

    private void validateTeacherCanViewClassroomSubject(Long teacherId, Long classroomId, Long subjectId,
                                                        int year, int semester) {
        Classroom classroom = classRoomRepository.findById(classroomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLASSROOM_NOT_FOUND));

        boolean isHomeroom = classroom.getHomeroomTeacher() != null
                && classroom.getHomeroomTeacher().getId().equals(teacherId)
                && classroom.getAcademicYear().equals(year)
                && classroom.getSemester().equals(semester);
        if (isHomeroom) return;

        boolean hasAssignment = subjectAssignmentRepository
                .findValidAssignment(teacherId, classroomId, subjectId, year, semester)
                .isPresent();
        if (!hasAssignment) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "해당 학급 과목의 통계 조회 권한이 없습니다");
        }
    }

    private void validateParentChild(Long parentId, Long studentId) {
        if (!parentMappingRepository.existsByParentIdAndStudentId(parentId, studentId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "연결된 자녀가 아닙니다");
        }
    }
}
