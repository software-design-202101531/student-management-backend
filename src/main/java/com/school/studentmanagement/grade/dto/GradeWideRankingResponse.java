package com.school.studentmanagement.grade.dto;

import com.school.studentmanagement.global.enums.GradeLevel;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class GradeWideRankingResponse {

    private Integer academicYear;
    private Integer semester;
    private Integer grade;       // 학년
    private Integer studentCount;

    private List<RankingEntry> rankings;

    @Getter
    @Builder
    public static class RankingEntry {
        private Integer rank;
        private Long studentId;
        private String studentName;
        private Integer classNum;       // 몇 반
        private Integer studentNum;     // 출석 번호
        private Double totalScore;
        private Double averageScore;
        private GradeLevel gradeLevel;
    }
}
