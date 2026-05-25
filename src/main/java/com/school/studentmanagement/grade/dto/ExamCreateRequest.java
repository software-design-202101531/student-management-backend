package com.school.studentmanagement.grade.dto;

import com.school.studentmanagement.global.enums.ExamType;
import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExamCreateRequest {

    @NotNull
    private Integer academicYear;

    @NotNull
    @Min(1) @Max(2)
    private Integer semester;

    @NotNull
    private ExamType examType;

    @NotBlank
    @Size(max = 50)
    private String name;

    @NotNull
    @Min(1)
    private Integer maxScore;

    @NotNull
    @DecimalMin("0.0") @DecimalMax("1.0")
    private Double weight;

    private LocalDate examDate;

    @Size(max = 500)
    private String coverage;

    @Builder
    public ExamCreateRequest(Integer academicYear, Integer semester, ExamType examType, String name,
                             Integer maxScore, Double weight, LocalDate examDate, String coverage) {
        this.academicYear = academicYear;
        this.semester = semester;
        this.examType = examType;
        this.name = name;
        this.maxScore = maxScore;
        this.weight = weight;
        this.examDate = examDate;
        this.coverage = coverage;
    }
}
