package com.school.studentmanagement.grade.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RadarChartResponse {

    private Long studentId;
    private String studentName;
    private Integer academicYear;
    private Integer semester;
    private List<SubjectRadarDto> subjects;

    @Getter
    @Builder
    public static class SubjectRadarDto {
        private Long subjectId;
        private String subjectName;
        private Double studentScore;   // 본인 학기 과목점수 (0~100, null이면 미입력)
        private Double classAverage;   // 학급 평균 (null이면 학급 데이터 없음)
    }
}
