package com.school.studentmanagement.analytics.dto;

import lombok.Builder;
import lombok.Getter;

// 과목 단위(여러 학생 가로지르는) 학습현황 대시보드 — analytics 스키마(사전 집계)에서만 읽는다.
@Getter
@Builder
public class SubjectAnalyticsOverviewResponse {

    private Long subjectId;
    private int academicYear;
    private int semester;

    // 성적 집계(student_subject_summary)
    private int studentCount;        // 해당 과목 성적이 집계된 학생 수
    private Double avgWeightedScore;  // 학생 가중점수의 평균 (데이터 없으면 null)
    private Double avgRawScore;       // 학생 원점수 평균의 평균 (데이터 없으면 null)
    private ScoreDistribution distribution; // 가중점수 구간 분포

    // 과제 제출 집계(student_submission_summary)
    private int assignedTotal;        // 부여 과제 수 합(학생×과제)
    private int submittedTotal;       // 제출 수 합
    private Double avgSubmissionRate; // 학생별 제출률의 평균 (데이터 없으면 null)

    // 가중점수 구간별 학생 수
    @Getter
    @Builder
    public static class ScoreDistribution {
        private int range90to100; // [90, 100]
        private int range80to89;  // [80, 90)
        private int range70to79;  // [70, 80)
        private int range60to69;  // [60, 70)
        private int below60;      // [0, 60)
    }
}
