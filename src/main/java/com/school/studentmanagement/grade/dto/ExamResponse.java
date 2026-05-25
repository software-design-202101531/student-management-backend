package com.school.studentmanagement.grade.dto;

import com.school.studentmanagement.global.enums.ExamType;
import com.school.studentmanagement.grade.entity.Exam;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class ExamResponse {

    private Long examId;
    private Integer academicYear;
    private Integer semester;
    private ExamType examType;
    private String name;
    private Integer maxScore;
    private Double weight;
    private LocalDate examDate;
    private String coverage;
    private boolean published;

    public static ExamResponse from(Exam exam) {
        return ExamResponse.builder()
                .examId(exam.getId())
                .academicYear(exam.getAcademicYear())
                .semester(exam.getSemester())
                .examType(exam.getExamType())
                .name(exam.getName())
                .maxScore(exam.getMaxScore())
                .weight(exam.getWeight())
                .examDate(exam.getExamDate())
                .coverage(exam.getCoverage())
                .published(exam.isPublished())
                .build();
    }
}
