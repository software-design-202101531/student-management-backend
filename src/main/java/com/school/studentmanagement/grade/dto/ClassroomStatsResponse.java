package com.school.studentmanagement.grade.dto;

import com.school.studentmanagement.global.enums.ExamType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ClassroomStatsResponse {

    private Long classroomId;
    private Long examId;
    private String examName;
    private ExamType examType;
    private Long subjectId;
    private String subjectName;
    private Integer maxScore;

    private Integer studentCount;            // 입력된 점수 수
    private Double averageScore;
    private Double standardDeviation;        // 모표준편차 (STDDEV_POP)
    private Integer maxValue;
    private Integer minValue;

    // 점수 분포: 0-9, 10-19, ..., 90-100 (100점은 마지막 구간 포함)
    private List<ScoreBin> distribution;

    @Getter
    @Builder
    public static class ScoreBin {
        private String range;
        private Integer count;
    }
}
