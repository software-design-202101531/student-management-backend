package com.school.studentmanagement.student.service;

import com.school.studentmanagement.global.enums.ExamType;
import com.school.studentmanagement.global.enums.RecordCategory;
import com.school.studentmanagement.global.util.AcademicCalendarUtil;
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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentMyPageService {

    private final StudentGradeRepository studentGradeRepository;
    private final StudentSemesterStatRepository semesterStatRepository;
    private final StudentRecordRepository studentRecordRepository;
    private final AcademicCalendarUtil academicCalendarUtil;

    public StudentMyGradeResponse getMyGrades(Long studentId, Integer academicYear, Integer semester) {
        int year = academicYear != null ? academicYear : academicCalendarUtil.getCurrentAcademicYear();
        int sem = semester != null ? semester : academicCalendarUtil.getCurrentSemester();

        StudentSemesterStat stat = semesterStatRepository
                .findByStudentIdAndAcademicYearAndSemester(studentId, year, sem)
                .orElse(null);

        List<StudentGrade> grades = studentGradeRepository
                .findByStudentIdAndAcademicYearAndSemester(studentId, year, sem);

        Map<ExamType, List<StudentMyGradeResponse.SubjectScoreDto>> byExamType = grades.stream()
                .collect(Collectors.groupingBy(
                        g -> g.getExam().getExamType(),
                        Collectors.mapping(
                                g -> StudentMyGradeResponse.SubjectScoreDto.builder()
                                        .subjectName(g.getSubject().getName())
                                        .rawScore(g.getRawScore())
                                        .build(),
                                Collectors.toList()
                        )
                ));

        List<StudentMyGradeResponse.ExamGradeDto> examGrades = byExamType.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().name()))
                .map(e -> StudentMyGradeResponse.ExamGradeDto.builder()
                        .examType(e.getKey())
                        .subjects(e.getValue())
                        .build())
                .toList();

        return StudentMyGradeResponse.builder()
                .academicYear(year)
                .semester(sem)
                .totalScore(stat != null ? stat.getTotalScore() : 0)
                .averageScore(stat != null ? stat.getAverageScore() : 0.0)
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
