package com.school.studentmanagement.analytics.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

// 학생 학습현황 대시보드 — analytics 스키마(사전 집계)에서만 읽는다(운영 DB 미접근).
@Getter
@Builder
public class StudentAnalyticsOverviewResponse {

    private Long studentId;
    private int academicYear;
    private int semester;
    private List<SubjectStat> subjects;
    private AttendanceStat attendance;   // 데이터 없으면 null
    private FeedbackStat feedback;       // 데이터 없으면 null
    private List<SubmissionStat> submissions;

    @Getter
    @Builder
    public static class SubjectStat {
        private Long subjectId;
        private Double weightedScore;
        private Double avgRawScore;
        private int gradeCount;
    }

    @Getter
    @Builder
    public static class AttendanceStat {
        private int absentCount;
        private int lateCount;
        private int earlyLeaveCount;
        private int totalRecords;     // 출결 예외(결석/지각/조퇴) 기록 수
        private Integer schoolDays;    // 학기 수업일수(분모). 데이터 없으면 null
        private Double attendanceRate; // (수업일-결석)/수업일. 데이터 없으면 null
    }

    @Getter
    @Builder
    public static class FeedbackStat {
        private int totalCount;
        private int publicCount;
    }

    @Getter
    @Builder
    public static class SubmissionStat {
        private Long subjectId;
        private int assignedCount;
        private int submittedCount;
        private int lateCount;
        private Double submissionRate;
    }
}
