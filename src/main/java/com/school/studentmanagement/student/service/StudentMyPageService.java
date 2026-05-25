package com.school.studentmanagement.student.service;

import com.school.studentmanagement.global.enums.RecordCategory;
import com.school.studentmanagement.global.util.AcademicCalendarUtil;
import com.school.studentmanagement.grade.entity.Exam;
import com.school.studentmanagement.grade.entity.StudentGrade;
import com.school.studentmanagement.grade.entity.StudentSemesterStat;
import com.school.studentmanagement.grade.repository.StudentGradeRepository;
import com.school.studentmanagement.grade.repository.StudentSemesterStatRepository;
import com.school.studentmanagement.record.entity.StudentRecord;
import com.school.studentmanagement.record.repository.StudentRecordRepository;
import com.school.studentmanagement.student.dto.StudentMyGradeResponse;
import com.school.studentmanagement.student.dto.StudentMyRecordResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentMyPageService {
    // 학생 본인의 성적과 생활기록부를 조회

    private final StudentGradeRepository studentGradeRepository;
    private final StudentSemesterStatRepository semesterStatRepository;
    private final StudentRecordRepository studentRecordRepository;
    private final AcademicCalendarUtil academicCalendarUtil;

    // 성적 조회 메서드
    public StudentMyGradeResponse getMyGrades(Long studentId, Integer academicYear, Integer semester) {
        int year = academicYear != null ? academicYear : academicCalendarUtil.getCurrentAcademicYear();
        int sem = semester != null ? semester : academicCalendarUtil.getCurrentSemester();

        StudentSemesterStat stat = semesterStatRepository
                .findByStudentIdAndAcademicYearAndSemester(studentId, year, sem)
                .orElse(null);

        // published=true인 시험만, 시험일 오름차순
        List<StudentGrade> grades = studentGradeRepository
                .findPublishedByStudentIdAndAcademicYearAndSemester(studentId, year, sem);

        Map<Long, List<StudentGrade>> byExamId = new LinkedHashMap<>();
        for (StudentGrade g : grades) {
            byExamId.computeIfAbsent(g.getExam().getId(), k -> new ArrayList<>()).add(g);
        }

        List<StudentMyGradeResponse.ExamGradeDto> examGrades = byExamId.values().stream()
                .map(list -> {
                    Exam exam = list.get(0).getExam();
                    List<StudentMyGradeResponse.SubjectScoreDto> subjects = list.stream()
                            .map(g -> StudentMyGradeResponse.SubjectScoreDto.builder()
                                    .subjectName(g.getSubject().getName())
                                    .rawScore(g.getRawScore())
                                    .attendanceStatus(g.getAttendanceStatus())
                                    .build())
                            .toList();
                    return StudentMyGradeResponse.ExamGradeDto.builder()
                            .examId(exam.getId())
                            .examType(exam.getExamType())
                            .examName(exam.getName())
                            .examDate(exam.getExamDate())
                            .maxScore(exam.getMaxScore())
                            .coverage(exam.getCoverage())
                            .subjects(subjects)
                            .build();
                })
                .toList();

        return StudentMyGradeResponse.builder()
                .academicYear(year)
                .semester(sem)
                .totalScore(stat != null ? stat.getTotalScore() : 0.0)
                .averageScore(stat != null ? stat.getAverageScore() : 0.0)
                .gradeLevel(stat != null ? stat.getGradeLevel() : null)
                .examGrades(examGrades)
                .build();
    }

    public StudentMyRecordResponse getMyRecords(Long studentId, Integer academicYear, Integer semester) {
        int year = academicYear != null ? academicYear : academicCalendarUtil.getCurrentAcademicYear();
        int sem = semester != null ? semester : academicCalendarUtil.getCurrentSemester();

        List<StudentRecord> records = studentRecordRepository
                .findAllByStudentIdAndAcademicYearAndSemester(studentId, year, sem);

        String behaviorContent = records.stream()
                .filter(r -> r.getRecordCategory() == RecordCategory.BEHAVIOR_OPINION)
                .map(StudentRecord::getContent)
                .findFirst()
                .orElse("");

        List<StudentMyRecordResponse.SubjectRecordDto> subjectRecords = records.stream()
                .filter(r -> r.getRecordCategory() == RecordCategory.SUBJECT_OPINION)
                .map(r -> StudentMyRecordResponse.SubjectRecordDto.builder()
                        .subjectName(r.getSubject().getName())
                        .content(r.getContent())
                        .build())
                .toList();

        return StudentMyRecordResponse.builder()
                .academicYear(year)
                .semester(sem)
                .behaviorRecord(behaviorContent)
                .subjectRecords(subjectRecords)
                .build();
    }
}
