package com.school.studentmanagement.grade.dto;

import com.school.studentmanagement.global.enums.GradeLevel;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ClassroomRankingResponse {

    private Long classroomId;
    private Integer academicYear;
    private Integer semester;
    private Integer classSize;

    // 학기 평균 내림차순. 동점이면 같은 등수 (RANK 의미: 1,1,3,...)
    private List<RankingEntry> rankings;

    @Getter
    @Builder
    public static class RankingEntry {
        private Integer rank;
        private Long studentId;
        private String studentName;
        private Integer studentNum;
        private Double totalScore;
        private Double averageScore;
        private GradeLevel gradeLevel;
    }
}
