package com.school.studentmanagement.grade.service;

import com.school.studentmanagement.global.enums.GradeLevel;
import com.school.studentmanagement.grade.entity.StudentSemesterStat;
import com.school.studentmanagement.grade.repository.StudentGradeRepository;
import com.school.studentmanagement.grade.repository.StudentSemesterStatRepository;
import com.school.studentmanagement.grade.repository.SubjectScoreAggregation;
import com.school.studentmanagement.student.entity.Student;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 학기 통계(StudentSemesterStat) 재계산 단일 책임 컴포넌트.
 * <p>
 * StudentGradeService와 SemesterClosureService 둘 다에서 호출되므로
 * 순환 의존성을 피하기 위해 별도 컴포넌트로 분리.
 */
@Component
@RequiredArgsConstructor
public class SemesterStatRecalculator {

    private final StudentGradeRepository studentGradeRepository;
    private final StudentSemesterStatRepository semesterStatRepository;
    private final GradePolicyService gradePolicyService;

    @Transactional
    public void refresh(Student student, Integer academicYear, Integer semester) {
        List<SubjectScoreAggregation> subjectScores = studentGradeRepository
                .aggregateSubjectScoresByStudentAndSemester(student.getId(), academicYear, semester);

        if (subjectScores.isEmpty()) return;

        double total = subjectScores.stream()
                .mapToDouble(SubjectScoreAggregation::getSubjectScore)
                .sum();
        double average = total / subjectScores.size();
        GradeLevel gradeLevel = gradePolicyService.evaluate(average);

        semesterStatRepository.findByStudentIdAndAcademicYearAndSemester(student.getId(), academicYear, semester)
                .ifPresentOrElse(
                        stat -> stat.updateStats(total, average, gradeLevel),
                        () -> semesterStatRepository.save(StudentSemesterStat.builder()
                                .student(student)
                                .academicYear(academicYear)
                                .semester(semester)
                                .totalScore(total)
                                .averageScore(average)
                                .gradeLevel(gradeLevel)
                                .build())
                );
    }
}
