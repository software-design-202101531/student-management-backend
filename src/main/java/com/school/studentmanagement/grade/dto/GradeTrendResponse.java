package com.school.studentmanagement.grade.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class GradeTrendResponse {

    private Long studentId;
    private Integer fromYear;
    private Integer fromSemester;
    private Integer toYear;
    private Integer toSemester;

    // 과목별 라인 (차트 그리기 쉬운 형태)
    private List<SubjectTrendDto> subjects;

    @Getter
    @Builder
    public static class SubjectTrendDto {
        private Long subjectId;
        private String subjectName;
        private List<TrendPoint> points;
    }

    @Getter
    @Builder
    public static class TrendPoint {
        private Integer academicYear;
        private Integer semester;
        private Double semesterScore;
    }
}
